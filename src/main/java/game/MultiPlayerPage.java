package game;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

import game.NetworkManager.GameState;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class MultiPlayerPage {

    private App app;
    private StackPane rootPane;

    // 遊戲相關變數
    private ImageView localPlayer; // 原來的 player, 現在明確為本地玩家
    private ArrayList<ImageView> localObstacles = new ArrayList<>(); // 原來的 obstacles, 現在明確為本地玩家畫面上的障礙物
    private int currentLane = 1;
    private final int lanes = 4;
    private final int laneWidth = 70;
    private static final double TRACK_START_Y = 800 * 0.28;

    private double speed;
    private int score;
    private int lives;
    private boolean gameOver;

    private long lastObstacleTime;
    private int lastUsedLane;

    private Text scoreText, lifeText;
    private Random rand = new Random();
    private AnimationTimer timer;
    // private Pane gamePane; // 移除 gamePane，直接使用 player1Pane 和 player2Pane
    private RaceTrackCanvas backgroundCanvas;

    // 玩家2的變數 (遠端玩家)
    private ImageView remotePlayer; // 原來的 player2, 現在明確為遠端玩家
    private ArrayList<ImageView> remoteObstacles = new ArrayList<>(); // 新增：用於遠端玩家畫面上的障礙物
    private int remotePlayerLane = 1; // 原來的 player2Lane
    private int remotePlayerScore = 0; // 原來的 player2Score
    private int remotePlayerLives = 3; // 原來的 player2Lives
    private Text remotePlayerScoreText, remotePlayerLifeText; // 原來的 player2ScoreText, player2LifeText

    private NetworkManager networkManager = new NetworkManager();
    private boolean isHost;

    // 網路同步相關變數
    private long lastNetworkSync = 0;
    private static final long NETWORK_SYNC_INTERVAL = 33; // ms (約 30 FPS)
    private static final long NETWORK_TIMEOUT = 2000; // 5秒逾時
    private long lastReceivedTime = System.currentTimeMillis();

    private static final String PLAYER1_IMAGE = "/image/redCar.png";
    private static final String PLAYER2_IMAGE = "/image/blueCar.png";
    private static final String[] OBSTACLE_IMAGES = {
            "/image/bananaPeel.png",
            "/image/can.png",
            "/image/garbage.png",
            "/image/redBlock.png",
            "/image/yellowBlock.png"

    };

    private boolean localEliminated = false; // 本地玩家是否淘汰
    private boolean remoteEliminated = false; // 遠端玩家是否淘汰

    // 記錄兩個玩家的 Pane
    private Pane player1Pane; // 本地玩家的遊戲畫面
    private Pane player2Pane; // 遠端玩家的遊戲畫面

    // 記錄當前顯示的 readyBox
    private VBox activeReadyBox;

    // 客戶端旗標
    private volatile boolean clientWaitingForStartSignal = false;
    private volatile boolean clientGameHasStarted = false;
    private volatile boolean clientListenerShouldExit = false; // 新增終止監聽的標誌

    public MultiPlayerPage(App app) {
        this.app = app;

        // 每次建立新的網路管理器
        this.networkManager = new NetworkManager();

        // 將網路管理器設置到 App 中，方便後續訪問
        if (app != null) {
            app.setCurrentNetworkManager(this.networkManager);
        }
    }

    @SuppressWarnings("exports")
    public Parent createContent() {
        rootPane = new StackPane();
        rootPane.setPrefSize(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);    
        showWaitingPage();
        return rootPane;
    }

    // 顯示等待配對頁面
    public void showWaitingPage() {
        StackPane root = new StackPane();
        root.setPrefSize(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);

        // 綁定背景畫布的大小
        backgroundCanvas = new RaceTrackCanvas(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
        backgroundCanvas.widthProperty().bind(root.widthProperty());
        backgroundCanvas.heightProperty().bind(root.heightProperty());

        VBox waitingLayout = new VBox(20);
        waitingLayout.setMinWidth(App.WINDOW_WIDTH / 2);
        waitingLayout.setMaxWidth(App.WINDOW_WIDTH / 2);
        waitingLayout.setMinHeight(App.WINDOW_HEIGHT / 2.5);
        waitingLayout.setMaxHeight(App.WINDOW_HEIGHT / 2.5);
        waitingLayout.setAlignment(Pos.CENTER);
        waitingLayout.setPrefSize(App.WINDOW_WIDTH / 2, App.WINDOW_HEIGHT / 2.5);
        waitingLayout.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.7); -fx-padding: 20px; -fx-border-radius: 10px; -fx-background-radius: 10px;");

        HBox btHbox = new HBox(50);
        btHbox.setAlignment(Pos.BOTTOM_CENTER);
        VBox joinLayout = new VBox(20);
        joinLayout.setAlignment(Pos.CENTER);

        // 添加內容
        Button hostButton = new Button("建立遊戲");
        UIUtils.applywaitButton(hostButton);
        Button joinButton = new Button("加入遊戲");
        UIUtils.applywaitButton(joinButton);
        TextField ipField = new TextField();
        ipField.setMinWidth(150);
        ipField.setMaxWidth(150);
        ipField.setPrefWidth(150);
        ipField.setPromptText("輸入主機IP");

        // 新增返回首頁按鈕
        Button backButton = new Button("返回首頁");
        UIUtils.applybackButton(backButton);
        backButton.setOnAction(e -> app.switchToHomePage());

        hostButton.setOnAction(e -> {
            new Thread(() -> {
                try {
                    networkManager.createHost(12345);
                    Platform.runLater(() -> {
                        showGameContent(true);
                        showReadyScreen();
                    });
                } catch (IOException ex) {
                    Platform.runLater(() -> showError("建立遊戲失敗: " + ex.getMessage()));
                    ex.printStackTrace();
                }
            }).start();
        });

        // 驗證 IP 地址
        joinButton.setOnAction(e -> {
            String ipAddress = ipField.getText();
            if (validateIP(ipAddress)) {
                new Thread(() -> {
                    try {
                        networkManager.joinGame(ipAddress, 12345);
                        Platform.runLater(() -> {
                            showGameContent(false);
                            showReadyScreen();
                        });
                    } catch (IOException ex) {
                        Platform.runLater(() -> showError("加入遊戲失敗: " + ex.getMessage()));
                        ex.printStackTrace();
                    }
                }).start();
            } else {
                showError("IP 地址格式無效");
            }
        });
        joinLayout.getChildren().addAll(ipField, joinButton);
        btHbox.getChildren().addAll(hostButton, joinLayout);

        waitingLayout.getChildren().addAll(
                btHbox,
                backButton);

        // 將背景和內容添加到根容器
        root.getChildren().addAll(backgroundCanvas, waitingLayout);
        StackPane.setAlignment(waitingLayout, Pos.CENTER);
        rootPane.getChildren().setAll(root);

        showHostIPAddress(waitingLayout);
    }

    // 顯示主機 IP 位址
    private void showHostIPAddress(VBox waitingLayout) {
        try {
            String hostIP = InetAddress.getLocalHost().getHostAddress();
            Text ipText = new Text("你的 IP 地址: " + hostIP);
            ipText.setStyle("-fx-fill: red; -fx-font-size: 16px; -fx-font-weight: bold;");

            waitingLayout.getChildren().add(ipText);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    // 配對完成後呼叫此方法顯示遊戲內容
    public void showGameContent(boolean isHost) {
        this.isHost = isHost;
        gameOver = false;
        speed = 10;
        score = 0;
        lives = 3;
        lastObstacleTime = 0;
        lastUsedLane = -1;
        localObstacles.clear();
        remoteObstacles.clear();

        System.out.println("顯示遊戲畫面，主機: " + isHost);

        HBox root = new HBox(5);
        root.setPrefSize(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);

        String localCarImage = isHost ? PLAYER1_IMAGE : PLAYER2_IMAGE;
        String remoteCarImage = isHost ? PLAYER2_IMAGE : PLAYER1_IMAGE;

        player1Pane = createPlayerPane(isHost ? "玩家1" : "玩家2", localCarImage, true);
        player2Pane = createPlayerPane(isHost ? "玩家2" : "玩家1", remoteCarImage, false);
        root.getChildren().addAll(player1Pane, player2Pane);

        rootPane.getChildren().clear();
        rootPane.getChildren().add(root);

        System.out.println("遊戲畫面已設置，player1Pane: " + (player1Pane != null) + ", player2Pane: " + (player2Pane != null));
    }

    private Pane createPlayerPane(String playerName, String carImagePath, boolean isPlayer1) {
        Pane playerPane = new Pane();
        playerPane.setPrefSize(App.WINDOW_WIDTH / 2, App.WINDOW_HEIGHT);

        RaceTrackCanvas backgroundCanvas = new RaceTrackCanvas(App.WINDOW_WIDTH / 2, App.WINDOW_HEIGHT);
        backgroundCanvas.widthProperty().bind(playerPane.widthProperty());
        backgroundCanvas.heightProperty().bind(playerPane.heightProperty());

        // 玩家
        Image carImage = null;
        try {
            carImage = new Image(getClass().getResourceAsStream(carImagePath));
            if (carImage != null && carImage.isError()) {
                System.err.println("警告：載入玩家圖片發生錯誤: " + carImagePath);
                carImage = null;
            }
        } catch (Exception e) {
            System.err.println("錯誤：無法載入玩家圖片: " + carImagePath + " - " + e.getMessage());
            e.printStackTrace();
            carImage = null;
        }

        if (carImage == null) {
            System.err.println("警告：玩家圖片 " + carImagePath + " 載入失敗。嘗試使用預設紅色方塊。");
            try {
                carImage = new Image(getClass().getResourceAsStream("/image/redBlock.png"));
                if (carImage != null && carImage.isError()) {
                    System.err.println("致命錯誤：載入備用玩家圖片也失敗。");
                    carImage = null;
                }
            } catch (Exception e) {
                System.err.println("致命錯誤：無法載入備用玩家圖片: " + e.getMessage());
                carImage = null;
            }
        }

        ImageView playerImg;
        if (carImage != null) {
            playerImg = new ImageView(carImage);
        } else {
            System.err.println("嚴重錯誤：無法為玩家創建有效的 ImageView，將使用一個空的 ImageView。");
            playerImg = new ImageView();
        }

        playerImg.setFitWidth(60);
        playerImg.setFitHeight(90);
        playerImg.setLayoutY(playerPane.getPrefHeight() * 0.7);

        // 玩家資訊
        String scoreLabel, lifeLabel;
        if (isPlayer1) {
            // 本地玩家
            scoreLabel = isHost ? "玩家1分數: " : "玩家2分數: ";
            lifeLabel = isHost ? "玩家1生命: " : "玩家2生命: ";
        } else {
            // 遠端玩家
            scoreLabel = isHost ? "玩家2分數: " : "玩家1分數: ";
            lifeLabel = isHost ? "玩家2生命: " : "玩家1生命: ";
        }

        Text playerText = new Text(playerName);
        playerText.setStyle("-fx-font-size: 18px;");
        Text scoreText = new Text(scoreLabel + "0");
        scoreText.setStyle("-fx-font-size: 16px;");
        Text lifeText = new Text(lifeLabel + "3");
        lifeText.setStyle("-fx-font-size: 16px;");

        VBox infoBox = new VBox(8, playerText, scoreText, lifeText);
        infoBox.setAlignment(Pos.TOP_LEFT);
        infoBox.setPadding(new Insets(10));
        infoBox.setStyle("-fx-background-color: rgba(255,255,255,0.7); -fx-background-radius: 10px;");
        infoBox.setLayoutX(15);
        infoBox.setLayoutY(15);

        if (isPlayer1) {
            this.localPlayer = playerImg;
            this.scoreText = scoreText;
            this.lifeText = lifeText;
            this.player1Pane = playerPane;
        } else {
            this.remotePlayer = playerImg;
            this.remotePlayerScoreText = scoreText;
            this.remotePlayerLifeText = lifeText;
            this.player2Pane = playerPane;
        }

        playerPane.getChildren().addAll(backgroundCanvas, playerImg, infoBox);
        return playerPane;
    }

    // 在 startNetworkSync 方法中添加更多的安全保護
    private Thread networkSyncThread;

    private void startNetworkSync() {
        if (networkSyncThread != null && networkSyncThread.isAlive()) {
            networkSyncThread.interrupt();
            System.out.println("中斷舊的網路同步執行緒");
        }

        networkSyncThread = new Thread(() -> {
            String threadName = (isHost ? "主機" : "客戶端") + "-網路同步";
            System.out.println(threadName + " 執行緒已啟動");

            long lastValidReceiveTime = System.currentTimeMillis();

            while (!gameOver) {
                try {
                    if (Thread.currentThread().isInterrupted()) {
                        System.out.println(threadName + " 執行緒被中斷");
                        break;
                    }

                    long now = System.currentTimeMillis();

                    // 控制同步頻率
                    if (now - lastNetworkSync >= NETWORK_SYNC_INTERVAL) {
                        sendGameState();
                        receiveGameState(); // 可能發生阻塞或異常
                        lastNetworkSync = now;
                        lastValidReceiveTime = now;
                    }

                    // 檢查是否長時間沒收到對方資訊
                    if (now - lastValidReceiveTime > NETWORK_TIMEOUT) {
                        System.err.println(threadName + " 連線逾時: " + (now - lastValidReceiveTime) + "ms");
                        throw new IOException("連線逾時，超過 " + NETWORK_TIMEOUT + "ms 未收到數據");
                    }

                    Thread.sleep(5); // 釋放CPU
                } catch (InterruptedException ie) {
                    System.out.println(threadName + " 執行緒被中斷");
                    break;
                } catch (IOException e) {
                    System.err.println(threadName + " 發生錯誤: " + e.getMessage());
                    e.printStackTrace();
                    if (!gameOver) {
                        handleNetworkError();
                    }
                    break;
                } catch (Exception e) {
                    System.err.println(threadName + " 未知錯誤: " + e.getMessage());
                    e.printStackTrace();
                    if (!gameOver) {
                        handleNetworkError();
                    }
                    break;
                }
            }
            System.out.println(threadName + " 執行緒已終止。gameOver = " + gameOver);
        }, "NetworkSyncThread");

        networkSyncThread.setDaemon(true);
        networkSyncThread.start();
    }

    // 新增障礙物同步方法 (此方法在客戶端 receiveGameState 中調用)
    private void syncObstacles(ArrayList<NetworkManager.SerializableObstacle> hostObstacles) {
        System.out.println(
                "客戶端: syncObstacles - 開始同步障礙物。收到主機 " + (hostObstacles != null ? hostObstacles.size() : 0) + " 個障礙物。");

        Platform.runLater(() -> {
            // 1. 清除舊的本地障礙物
            player1Pane.getChildren().removeAll(localObstacles);
            localObstacles.clear();
            System.out.println("客戶端: syncObstacles - 舊本地障礙物已從 player1Pane 移除並清除本地列表。");

            // 2. 清除舊的遠端障礙物
            player2Pane.getChildren().removeAll(remoteObstacles);
            remoteObstacles.clear();
            System.out.println("客戶端: syncObstacles - 舊遠端障礙物已從 player2Pane 移除並清除遠端列表。");

            // 3. 添加新的障礙物到兩個 Pane
            if (hostObstacles != null) {
                for (NetworkManager.SerializableObstacle so : hostObstacles) {
                    try {
                        ImageView newObsForLocalPlayer = so.toImageView();
                        if (newObsForLocalPlayer != null) {
                            localObstacles.add(newObsForLocalPlayer);
                            player1Pane.getChildren().add(newObsForLocalPlayer);
                        } else {
                            System.err.println("警告：客戶端: syncObstacles - 為本地玩家創建 ImageView 返回 null。");
                        }

                        // 為遠端玩家畫面創建 ImageView (位置需要根據 player2Pane 調整)
                        ImageView newObsForRemotePlayer = so.toImageView();
                        if (newObsForRemotePlayer != null) {
                            remoteObstacles.add(newObsForRemotePlayer);
                            player2Pane.getChildren().add(newObsForRemotePlayer);
                        } else {
                            System.err.println("警告：客戶端: syncObstacles - 為遠端玩家創建 ImageView 返回 null。");
                        }

                    } catch (Exception e) {
                        System.err.println("錯誤：客戶端: syncObstacles - 在創建 ImageView 時發生錯誤: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            System.out.println(
                    "客戶端: syncObstacles - 新增了 " + (hostObstacles != null ? hostObstacles.size() : 0) + " 個障礙物到兩個遊戲面板。");
            System.out.println("客戶端: syncObstacles - 障礙物同步完成。當前本地障礙物數量: " + localObstacles.size() + ", 遠端障礙物數量: "
                    + remoteObstacles.size());
        });
    }

    // 在連線遊戲結束後調用此方法
    private void startGame() {
        if (player1Pane == null || player2Pane == null) {
            showError("遊戲面板未初始化，無法啟動遊戲。");
            return;
        }

        Text countdownText = new Text();
        countdownText.setStyle("-fx-font-size: 48px;");
        countdownText.layoutXProperty().bind(player1Pane.widthProperty().divide(2).subtract(50));
        countdownText.layoutYProperty().bind(player1Pane.heightProperty().divide(2));
        player1Pane.getChildren().add(countdownText);

        // 雙方確認準備好
        if (isHost) {
            new Thread(() -> {
                try {
                    System.out.println("主機：等待客戶端準備好信號...");
                    GameState clientState = networkManager.receiveCompressedGameState();
                    System.out.println("主機：收到客戶端準備好信號，isReady = " + clientState.isReady);
                    if (clientState.isReady) {
                        // 發送開始倒計時信號
                        GameState startSignal = new GameState();
                        startSignal.gameStarting = true;
                        networkManager.sendCompressedGameState(startSignal);
                        Platform.runLater(() -> startCountdown(countdownText));
                    }
                } catch (Exception e) {
                    System.err.println("主機：等待客戶端準備時發生錯誤: " + e.getMessage());
                    e.printStackTrace();
                    handleNetworkError();
                }
            }).start();
        } else {
            new Thread(() -> {
                try {
                    GameState readySignal = new GameState();
                    readySignal.isReady = true;
                    networkManager.sendCompressedGameState(readySignal);
                    System.out.println("客戶端：已發送準備好信號。");
                    
                    System.out.println("客戶端：等待主機開始信號...");
                    GameState hostState = networkManager.receiveCompressedGameState();
                    System.out.println("客戶端：收到主機開始信號，gameStarting = " + hostState.gameStarting);
                    if (hostState.gameStarting) {
                        Platform.runLater(() -> startCountdown(countdownText));
                    }
                } catch (Exception e) {
                    System.err.println("客戶端：等待主機開始信號時發生錯誤: " + e.getMessage());
                    e.printStackTrace();
                    handleNetworkError();
                }
            }).start();
        }
    }

    private void startCountdown(Text countdownText) {
        if (timer != null) {
            timer.stop();
            timer = null;
        }

        // 開始倒數
        new Thread(() -> {
            try {
                Platform.runLater(() -> updatePlayerPosition());
                for (int i = 3; i > 0; i--) {
                    final int count = i;
                    Platform.runLater(() -> countdownText.setText(String.valueOf(count)));
                    Thread.sleep(1000);
                }
                Platform.runLater(() -> {
                    countdownText.setText("開始！");
                });
                Thread.sleep(1000);
                Platform.runLater(() -> {
                    player1Pane.getChildren().remove(countdownText);

                    clientWaitingForStartSignal = false;
                    clientListenerShouldExit = true;

                    System.out.println("客戶端: 準備啟動遊戲循環和網路同步...");                    
                    startGameLoop();
                    startNetworkSync();
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void startGameLoop() {
        if (timer != null) {
            timer.stop();
        }

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!gameOver) {
                    update();
                }
            }
        };
        timer.start();
    }

    public void handleKeyPress(KeyEvent event) {
        if (gameOver) {
            return;
        }
        KeyCode code = event.getCode();

        // 只控制本地玩家
        if (code == KeyCode.LEFT && currentLane > 0) {
            currentLane--;
            updatePlayerPosition();
        }
        if (code == KeyCode.RIGHT && currentLane < lanes - 1) {
            currentLane++;
            updatePlayerPosition();
        }
    }

    private void updatePlayerPosition() {
        if (player1Pane == null || localPlayer == null) {
            return;
        }
        double paneWidth = player1Pane.getWidth();
        double paneHeight = player1Pane.getHeight();
        double playerY = paneHeight - localPlayer.getFitHeight() - 50;
        double trackWidth = getTrackWidthAtY(playerY, paneHeight);
        double laneWidth = trackWidth / lanes;
        double trackCenterX = paneWidth / 2.0;
        double trackLeftX = trackCenterX - trackWidth / 2.0;

        double playerX = trackLeftX + currentLane * laneWidth + (laneWidth - localPlayer.getFitWidth()) / 2.0;
        localPlayer.setLayoutX(playerX);
        localPlayer.setLayoutY(playerY);
    }

    private void updateRemotePlayerPosition() {
        if (player2Pane == null || remotePlayer == null) {
            return;
        }
        double paneWidth = player2Pane.getWidth();
        double paneHeight = player2Pane.getHeight();
        double playerY = paneHeight - remotePlayer.getFitHeight() - 50;
        double trackWidth = getTrackWidthAtY(playerY, paneHeight);
        double laneWidth = trackWidth / lanes;
        double trackCenterX = paneWidth / 2.0;
        double trackLeftX = trackCenterX - trackWidth / 2.0;

        double playerX = trackLeftX + remotePlayerLane * laneWidth + (laneWidth - remotePlayer.getFitWidth()) / 2.0;
        remotePlayer.setLayoutX(playerX);
        remotePlayer.setLayoutY(playerY);
    }

    private double getTrackWidthAtY(double y, double paneHeight) {
        double TRACK_START_Y = paneHeight * 0.3;
        double TRACK_BOTTOM_Y = paneHeight;
        double TRACK_TOP_WIDTH = 200;
        double TRACK_BOTTOM_WIDTH = 400;
        double t = (y - TRACK_START_Y) / (TRACK_BOTTOM_Y - TRACK_START_Y);
        t = Math.max(0, Math.min(1, t));
        return TRACK_TOP_WIDTH + (TRACK_BOTTOM_WIDTH - TRACK_TOP_WIDTH) * t;
    }

    private void update() {    
        if (gameOver) {
            return;
        }
        
        if (localEliminated && remoteEliminated) {
            Platform.runLater(() -> checkGameOver());
            return;
        }

        // 主機端邏輯
        if (isHost) {
            if (localEliminated) {                
                Platform.runLater(() -> {
                    String localScoreLabel = "玩家1分數: ";
                    String localLifeLabel = "玩家1生命: ";
                    String remoteScoreLabel = "玩家2分數: ";
                    String remoteLifeLabel = "玩家2生命: ";

                    scoreText.setText(localScoreLabel + score);
                    lifeText.setText(localLifeLabel + lives);
                    remotePlayerScoreText.setText(remoteScoreLabel + remotePlayerScore);
                    remotePlayerLifeText.setText(remoteLifeLabel + remotePlayerLives);
                });
            } else {
                updateLocalPlayerAndObstacles();
            }
        }
        // 客戶端邏輯
        else {
            if (localEliminated) {                
                Platform.runLater(() -> {
                    String localScoreLabel = "玩家2分數: ";
                    String localLifeLabel = "玩家2生命: ";
                    String remoteScoreLabel = "玩家1分數: ";
                    String remoteLifeLabel = "玩家1生命: ";

                    scoreText.setText(localScoreLabel + score);
                    lifeText.setText(localLifeLabel + lives);
                    remotePlayerScoreText.setText(remoteScoreLabel + remotePlayerScore);
                    remotePlayerLifeText.setText(remoteLifeLabel + remotePlayerLives);
                });
            } else {
                generateObstacles();
                moveObstacles();
                checkCollision();
                Platform.runLater(() -> {
                    String localScoreLabel = "玩家2分數: ";
                    String localLifeLabel = "玩家2生命: ";
                    String remoteScoreLabel = "玩家1分數: ";
                    String remoteLifeLabel = "玩家1生命: ";

                    scoreText.setText(localScoreLabel + score);
                    lifeText.setText(localLifeLabel + lives);
                    remotePlayerScoreText.setText(remoteScoreLabel + remotePlayerScore);
                    remotePlayerLifeText.setText(remoteLifeLabel + remotePlayerLives);
                });
            }
        }
        updateRemotePlayer();
    }

    // 更新本地玩家和障礙物的畫面 (主機端)
    private void updateLocalPlayerAndObstacles() {
        generateObstacles();
        moveObstacles(); 
        checkCollision();

        // 更新分數顯示
        Platform.runLater(() -> {
            String localScoreLabel = isHost ? "玩家1分數: " : "玩家2分數: ";
            String localLifeLabel = isHost ? "玩家1生命: " : "玩家2生命: ";
            String remoteScoreLabel = isHost ? "玩家2分數: " : "玩家1分數: ";
            String remoteLifeLabel = isHost ? "玩家2生命: " : "玩家1生命: ";

            scoreText.setText(localScoreLabel + score);
            lifeText.setText(localLifeLabel + lives);
            remotePlayerScoreText.setText(remoteScoreLabel + remotePlayerScore);
            remotePlayerLifeText.setText(remoteLifeLabel + remotePlayerLives);
        });
    }

    // 更新遠端玩家的畫面 (在兩端都會執行)
    private void updateRemotePlayer() {        
        updateRemotePlayerPosition(); 
        Platform.runLater(() -> {
            String remoteScoreLabel = isHost ? "玩家2分數: " : "玩家1分數: ";
            String remoteLifeLabel = isHost ? "玩家2生命: " : "玩家1生命: ";
            remotePlayerScoreText.setText(remoteScoreLabel + remotePlayerScore);
            remotePlayerLifeText.setText(remoteLifeLabel + remotePlayerLives);
        });
    }

    // 修改障礙物生成邏輯，只由主機生成
    private void generateObstacles() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastObstacleTime < 1000) {
            return;
        }
        lastObstacleTime = currentTime;

        ArrayList<Integer> laneIndices = new ArrayList<>();
        for (int i = 0; i < lanes; i++) {
            laneIndices.add(i);
        }
        Collections.shuffle(laneIndices);

        int obstacleCount = 1 + rand.nextInt(2);
        int count = 0;

        for (int lane : laneIndices) {
            if (lane == lastUsedLane && laneIndices.size() > 1) {
                continue;
            }
            String imgPath = OBSTACLE_IMAGES[rand.nextInt(OBSTACLE_IMAGES.length)];
            ImageView obstacle = new ImageView();
            Image obsImg = null;
            try {
                obsImg = new Image(getClass().getResourceAsStream(imgPath));
                if (obsImg != null && obsImg.isError()) {
                    obsImg = null;
                }
            } catch (Exception e) {
                obsImg = null;
            }
            if (obsImg == null) {
                try {
                    obsImg = new Image(getClass().getResourceAsStream("/image/redBlock.png"));
                } catch (Exception e) {
                    System.err.println("無法載入任何障礙物圖片");
                    continue;
                }
            }

            obstacle.setImage(obsImg);
            obstacle.setFitWidth(60);
            obstacle.setFitHeight(60);
            obstacle.setUserData(new Object[] { imgPath, lane });

            double obstacleY = TRACK_START_Y;
            double trackWidth = getTrackWidthAtY(obstacleY, player1Pane.getHeight());
            double laneWidthVal = trackWidth / lanes;
            double trackCenterX = player1Pane.getWidth() / 2.0;
            double trackLeftX = trackCenterX - trackWidth / 2.0;
            double obstacleX = trackLeftX + lane * laneWidthVal + (laneWidthVal - obstacle.getFitWidth()) / 2.0;
            obstacle.setLayoutX(obstacleX);
            obstacle.setLayoutY(obstacleY);

            localObstacles.add(obstacle);

            final ImageView finalObstacle = obstacle;
            Platform.runLater(() -> player1Pane.getChildren().add(finalObstacle));

            lastUsedLane = lane;
            count++;
            if (count >= obstacleCount) {
                break;
            }
        }
    }

    private void moveObstacles() {        
        Iterator<ImageView> localIter = localObstacles.iterator();
        while (localIter.hasNext()) {
            ImageView obs = localIter.next();
            double newY = obs.getLayoutY() + speed;
            obs.setLayoutY(newY);

            Object userDataContent = obs.getUserData();
            int lane = -1;
            if (userDataContent instanceof Integer) {
                lane = (Integer) userDataContent;
            } else if (userDataContent instanceof Object[]) {
                Object[] data = (Object[]) userDataContent;
                if (data.length > 1 && data[1] instanceof Integer) {
                    lane = (Integer) data[1];
                }
            }
            if (lane != -1) {
                double trackWidth = getTrackWidthAtY(newY, player1Pane.getHeight());
                double laneWidthVal = trackWidth / lanes;
                double trackCenterX = player1Pane.getWidth() / 2.0;
                double trackLeftX = trackCenterX - trackWidth / 2.0;
                double obstacleX = trackLeftX + lane * laneWidthVal + (laneWidthVal - obs.getFitWidth()) / 2.0;
                obs.setLayoutX(obstacleX);
            }

            Boolean scored = (Boolean) obs.getProperties().getOrDefault("scored", false);

            if (newY > player1Pane.getHeight() && !scored) {               
                score++;
                if (score % 10 == 0) {
                    speed += 1.5;
                }
                Platform.runLater(() -> player1Pane.getChildren().remove(obs));
                localIter.remove();
            }
        }

        // 移動遠端玩家畫面上的障礙物
        Iterator<ImageView> remoteIter = remoteObstacles.iterator();
        while (remoteIter.hasNext()) {
            ImageView obs = remoteIter.next();
            double newY = obs.getLayoutY() + speed;
            obs.setLayoutY(newY);
            
            Object userDataContent = obs.getUserData();
            int lane = -1;
            if (userDataContent instanceof Integer) {
                lane = (Integer) userDataContent;
            } else if (userDataContent instanceof Object[]) {
                Object[] data = (Object[]) userDataContent;
                if (data.length > 1 && data[1] instanceof Integer) {
                    lane = (Integer) data[1];
                }
            }
            if (lane != -1) {
                double trackWidth = getTrackWidthAtY(newY, player2Pane.getHeight());
                double laneWidthVal = trackWidth / lanes;
                double trackCenterX = player2Pane.getWidth() / 2.0;
                double trackLeftX = trackCenterX - trackWidth / 2.0;
                double obstacleX = trackLeftX + lane * laneWidthVal + (laneWidthVal - obs.getFitWidth()) / 2.0;
                obs.setLayoutX(obstacleX);
            }
            if (newY > player2Pane.getHeight()) {
                Platform.runLater(() -> player2Pane.getChildren().remove(obs));
                remoteIter.remove();
            }
        }
    }

    // 修改碰撞檢查，只針對本地玩家
    private void checkCollision() {
        Iterator<ImageView> iter = localObstacles.iterator();
        while (iter.hasNext()) {
            ImageView obs = iter.next();

            if (localPlayer != null && obs != null
                    && localPlayer.getBoundsInParent().intersects(obs.getBoundsInParent())) {
                handleCollision(obs, iter, true); 
            }
        }
    }

    // 新增碰撞處理方法
    private void handleCollision(ImageView obs, Iterator<ImageView> iter, boolean isLocalPlayer) {
        Platform.runLater(() -> player1Pane.getChildren().remove(obs));
        iter.remove();

        if (isLocalPlayer) {
            lives--;
            Platform.runLater(() -> lifeText.setText("Lives: " + lives));
            if (lives <= 0 && !localEliminated) {
                localEliminated = true;
                onLocalEliminated();
            }
        } else {
            remotePlayerLives--;
            Platform.runLater(() -> remotePlayerLifeText.setText("Player 2 Lives: " + remotePlayerLives));
            if (remotePlayerLives <= 0 && !remoteEliminated) {
                remoteEliminated = true;
                onRemoteEliminated();
            }
        }
        speed = Math.max(10, speed - 1);
    }

    // 本地玩家淘汰時顯示等待畫面
    private void onLocalEliminated() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        Platform.runLater(() -> {            
            Text eliminatedText = new Text("你已淘汰\n等待其他玩家結束...");
            eliminatedText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            eliminatedText.setStyle("-fx-font-size: 28px; -fx-fill:red; -fx-font-weight: bold;");
            eliminatedText.setLayoutX(player1Pane.getWidth() / 2 - 150);
            eliminatedText.setLayoutY(player1Pane.getHeight() / 2);
            VBox contentBox = new VBox(20, eliminatedText);
            contentBox.setMinWidth(App.WINDOW_WIDTH / 4);
            contentBox.setMaxWidth(App.WINDOW_WIDTH / 4);
            contentBox.setMinHeight(App.WINDOW_HEIGHT / 4.5);
            contentBox.setMaxHeight(App.WINDOW_HEIGHT / 4.5);
            contentBox.setAlignment(Pos.CENTER);
            contentBox.setAlignment(Pos.CENTER);
            contentBox.setPadding(new Insets(50));
            contentBox.setStyle("-fx-background-color: rgba(255,255,255,0.7); -fx-background-radius: 20px;");

            double boxWidth = App.WINDOW_WIDTH / 4;
            double boxHeight = App.WINDOW_HEIGHT / 4.5;
            contentBox.setLayoutX((player1Pane.getWidth() - boxWidth) / 2);
            contentBox.setLayoutY((player1Pane.getHeight() - boxHeight) / 2);
            player1Pane.getChildren().add(contentBox);

            player1Pane.getChildren().removeAll(localObstacles);
            localObstacles.clear();

            checkGameOver();
        });


        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!gameOver) {
                    updateRemotePlayerOnly();
                }
            }
        };
        timer.start();
    }
    
    private void updateRemotePlayerOnly() {        
        Platform.runLater(() -> {
            String localScoreLabel = isHost ? "玩家1分數: " : "玩家2分數: ";
            String localLifeLabel = isHost ? "玩家1生命: " : "玩家2生命: ";
            String remoteScoreLabel = isHost ? "玩家2分數: " : "玩家1分數: ";
            String remoteLifeLabel = isHost ? "玩家2生命: " : "玩家1生命: ";

            scoreText.setText(localScoreLabel + score);
            lifeText.setText(localLifeLabel + lives);
            remotePlayerScoreText.setText(remoteScoreLabel + remotePlayerScore);
            remotePlayerLifeText.setText(remoteLifeLabel + remotePlayerLives);
            
            updateRemotePlayerPosition();
        });
    }

    private void onRemoteEliminated() {
        Platform.runLater(() -> {
            Text infoText = new Text("對方已淘汰，你可以繼續挑戰高分！");
            infoText.setStyle("-fx-font-size: 20px; -fx-fill: blue;-fx-font-weight: bold");
            infoText.setLayoutX(player1Pane.getWidth() / 2 - 120);
            infoText.setLayoutY(40);
            player1Pane.getChildren().add(infoText);

            player2Pane.getChildren().removeAll(remoteObstacles);
            remoteObstacles.clear();

            checkGameOver();
        });
    }

    private void checkGameOver() {
        System.out.println("[CheckGameOver] localEliminated=" + localEliminated + ", remoteEliminated="
                + remoteEliminated + ", gameOver=" + gameOver);

        if ((localEliminated && remoteEliminated) || gameOver) {
            if (!gameOver) {
                gameOver = true;
                System.out.println("[CheckGameOver] 雙方均已淘汰，設置 gameOver = true");
                try {
                    GameState endState = new GameState();
                    endState.playerLane = currentLane;
                    endState.score = score;
                    endState.lives = lives;
                    endState.gameEnded = true;
                    networkManager.sendCompressedGameState(endState);
                    System.out.println("[CheckGameOver] 已發送遊戲結束標記");
                } catch (IOException e) {
                    System.err.println("[CheckGameOver] 發送結束狀態時錯誤: " + e.getMessage());
                }
            }

            cleanup();

            Platform.runLater(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                }

                System.out.println("[CheckGameOver] 準備轉換到結算畫面");
                mulGameOverPage gameOverPage = new mulGameOverPage(app);
                int player1Score = isHost ? score : remotePlayerScore;
                int player2Score = isHost ? remotePlayerScore : score;
                gameOverPage.setScores(player1Score, player2Score);
                app.setScene(new javafx.scene.Scene(gameOverPage.createContent()));
            });
        }
    }

    // 修改遊戲結束方法
    private void endGame() {
        gameOver = true;

        if (timer != null) {
            timer.stop();
            timer = null;
        }

        Platform.runLater(() -> {
            Text gameOverText = new Text();
            gameOverText.setStyle("-fx-font-size: 24px; -fx-fill: white; -fx-stroke: black; -fx-stroke-width: 1;");

            // 判斷勝負
            String message;
            if (score > remotePlayerScore) {
                message = "玩家1 獲勝! 分數: " + score;
            } else if (remotePlayerScore > score) {
                message = "玩家2 獲勝! 分數: " + remotePlayerScore;
            } else {
                message = "平手! 分數: " + score;
            }

            gameOverText.setText(message);

            double textX = player1Pane.getWidth() / 2 - 100;
            double textY = player1Pane.getHeight() / 2;
            gameOverText.setLayoutX(textX);
            gameOverText.setLayoutY(textY);

            player1Pane.getChildren().add(gameOverText);

            Button returnButton = new Button("返回大廳");
            returnButton.setStyle("-fx-font-size: 18px;");
            returnButton.setLayoutX(textX);
            returnButton.setLayoutY(textY + 40);
            returnButton.setOnAction(e -> {
                // 關閉網路連線
                try {
                    networkManager.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                app.switchToHomePage();
            });

            player1Pane.getChildren().add(returnButton);
        });
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("錯誤");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void handleNetworkError() {
        System.err.println("處理網路錯誤。gameOver = " + gameOver);

        if (gameOver) {
            System.out.println("遊戲已結束，忽略網路錯誤");
            return;
        }

        gameOver = true;
        if (timer != null) {
            timer.stop();
        }

        Platform.runLater(() -> {
            showError("網路連線中斷");
            app.switchToHomePage();
        });
    }

    private void showReadyScreen() {
        if (player1Pane == null) {
            showError("遊戲畫面未初始化 (player1Pane is null)");
            return;
        }

        if (activeReadyBox != null && player1Pane.getChildren().contains(activeReadyBox)) {
            player1Pane.getChildren().remove(activeReadyBox);
        }
        clientGameHasStarted = false; 

        VBox newReadyBox = new VBox(20);
        newReadyBox.setAlignment(Pos.CENTER);
        newReadyBox.setStyle(
                "-fx-background-color: rgba(255,255,255,0.8); -fx-padding: 20px; -fx-border-radius: 15; -fx-background-radius: 15;");

        Text waitingText = new Text(isHost ? "等待玩家加入..." : "等待主機開始...");
        waitingText.setStyle("-fx-font-size: 24px; -fx-fill: #333;");

        Button readyButton = new Button("準備開始");
        readyButton.setStyle("-fx-font-size: 18px;");
        readyButton.setVisible(isHost); 

        this.activeReadyBox = newReadyBox;

        if (isHost) {
            readyButton.setOnAction(e -> {
                Platform.runLater(() -> { 
                    if (activeReadyBox != null && player1Pane.getChildren().contains(activeReadyBox)) {
                        player1Pane.getChildren().remove(activeReadyBox);
                        activeReadyBox = null;
                    }
                });
                new Thread(() -> {
                    try {
                        System.out.println("主機：發送開始遊戲信號。");
                        GameState startSignal = new GameState();
                        startSignal.gameStarting = true;
                        networkManager.sendCompressedGameState(startSignal);

                        Platform.runLater(() -> {
                            Text countdownText = new Text();
                            countdownText.setStyle("-fx-font-size: 48px;");
                            countdownText.layoutXProperty().bind(player1Pane.widthProperty().divide(2).subtract(50));
                            countdownText.layoutYProperty().bind(player1Pane.heightProperty().divide(2));
                            player1Pane.getChildren().add(countdownText);
                            startCountdown(countdownText);
                        });
                    } catch (Exception ex) {
                        System.err.println("主機：發送開始信號時發生錯誤: " + ex.getMessage());
                        ex.printStackTrace();
                        Platform.runLater(() -> handleNetworkError());
                    }
                }).start();
            });
        } else { // 客戶端邏輯
            clientWaitingForStartSignal = true;
            clientGameHasStarted = false;
            clientListenerShouldExit = false;

            new Thread(() -> {
                System.out.println("客戶端: 開始監聽主機開始信號");
                try {
                    while (clientWaitingForStartSignal && !gameOver && !clientGameHasStarted
                            && !clientListenerShouldExit) {
                        System.out.println("客戶端: 等待主機開始信號...");
                        GameState hostState = networkManager.receiveCompressedGameState();
                        System.out.println("客戶端: 收到遊戲狀態，gameStarting = " + hostState.gameStarting);

                        if (hostState != null && hostState.gameStarting) {
                            if (clientWaitingForStartSignal && !clientGameHasStarted && !clientListenerShouldExit) { // 再次檢查
                                clientWaitingForStartSignal = false;
                                clientListenerShouldExit = true; 
                                clientGameHasStarted = true;
                                System.out.println("客戶端: 收到主機開始信號，準備終止監聽線程");

                                Platform.runLater(() -> {
                                    if (activeReadyBox != null && player1Pane != null
                                            && player1Pane.getChildren().contains(activeReadyBox)) {
                                        player1Pane.getChildren().remove(activeReadyBox);
                                        activeReadyBox = null;
                                    }
                                    Text countdownText = new Text();
                                    countdownText.setStyle("-fx-font-size: 48px;");
                                    countdownText.layoutXProperty()
                                            .bind(player1Pane.widthProperty().divide(2).subtract(50));
                                    countdownText.layoutYProperty().bind(player1Pane.heightProperty().divide(2));
                                    if (player1Pane != null) {
                                        player1Pane.getChildren().add(countdownText);
                                        startCountdown(countdownText); 
                                    } else {
                                        System.err.println("客戶端: 遊戲面板未準備就緒");
                                        showError("客戶端錯誤：遊戲面板未準備就緒。");
                                    }
                                });
                            }
                            break; 
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    if (!clientListenerShouldExit) { 
                        System.err.println("客戶端: 接收開始信號時發生錯誤: " + e.getMessage());
                        e.printStackTrace();
                        if (clientWaitingForStartSignal && !clientGameHasStarted && !gameOver) {
                            Platform.runLater(() -> handleNetworkError());
                        }
                    } else {
                        System.out.println("客戶端監聽線程已主動終止，忽略異常");
                    }
                } finally {
                    System.out.println("客戶端: 監聽執行緒結束，等待狀態 = " + clientWaitingForStartSignal);
                    clientWaitingForStartSignal = false; 
                }
            }, "ClientStartSignalListener").start();
        }

        activeReadyBox.getChildren().addAll(waitingText, readyButton);

        double boxWidth = 200;
        double boxHeight = 100;
        activeReadyBox.setLayoutX((player1Pane.getPrefWidth() - boxWidth) / 2); 
        activeReadyBox.setLayoutY((player1Pane.getPrefHeight() - boxHeight) / 2);

        player1Pane.getChildren().add(activeReadyBox); 
    }

    // 修改障礙物發送邏輯
    private void sendGameState() {
        try {
            GameState localState = new GameState();
            localState.playerLane = currentLane;
            localState.score = score;
            localState.lives = lives;

            ArrayList<NetworkManager.SerializableObstacle> serializableObstacles = new ArrayList<>();
            for (ImageView obs : localObstacles) {
                Object userDataContent = obs.getUserData();
                String imgPath = null;
                int lane = -1;

                if (userDataContent instanceof Integer) {
                    lane = (Integer) userDataContent;
                    imgPath = "/image/redBlock.png"; 
                } else if (userDataContent instanceof Object[]) {
                    Object[] data = (Object[]) userDataContent;
                    if (data.length > 0 && data[0] instanceof String) {
                        imgPath = (String) data[0];
                    }
                    if (data.length > 1 && data[1] instanceof Integer) {
                        lane = (Integer) data[1];
                    }
                }

                if (imgPath != null && lane >= 0) {
                    serializableObstacles.add(new NetworkManager.SerializableObstacle(obs, imgPath, lane));
                }
            }
            localState.obstacles = serializableObstacles;
            if (localEliminated && remoteEliminated) {
                localState.gameEnded = true; 
            }

            networkManager.sendCompressedGameState(localState);
        } catch (IOException e) {
            System.err.println("發送遊戲狀態時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            handleNetworkError();
        }
    }

    // 修改障礙物接收邏輯
    private void receiveGameState() {
        try {
            GameState remoteState = networkManager.receiveCompressedGameState();
            
            if (remoteState.gameEnded) {
                gameOver = true;
                Platform.runLater(() -> checkGameOver());
                return;
            }

            if (isHost) {
                remotePlayerLane = remoteState.playerLane;
                remotePlayerScore = remoteState.score;
                remotePlayerLives = remoteState.lives;
                if (remoteState.obstacles != null) {
                    Platform.runLater(() -> syncRemoteObstacles(remoteState.obstacles));
                }
                if (remotePlayerLives <= 0 && !remoteEliminated) {
                    remoteEliminated = true;
                    onRemoteEliminated();
                    
                    if (localEliminated) {
                        Platform.runLater(() -> checkGameOver());
                    }
                }
                Platform.runLater(() -> updateRemotePlayerPosition());
            } else {
                if (remoteState.lives <= 0 && !remoteEliminated) {
                    remoteEliminated = true;
                    onRemoteEliminated();

                    if (localEliminated) {
                        Platform.runLater(() -> checkGameOver());
                    }
                }
                if (lives <= 0 && !localEliminated) {
                    localEliminated = true;
                    onLocalEliminated();

                    if (remoteEliminated) {
                        Platform.runLater(() -> checkGameOver());
                    }
                }
                Platform.runLater(() -> updatePlayerPosition());
                
                remotePlayerLane = remoteState.playerLane;
                remotePlayerScore = remoteState.score;
                remotePlayerLives = remoteState.lives;                
                if (remoteState.obstacles != null) {
                    Platform.runLater(() -> syncRemoteObstacles(remoteState.obstacles));
                }
                Platform.runLater(() -> updateRemotePlayerPosition());
            }

            // 更新分數和生命顯示
            Platform.runLater(() -> {
                String localScoreLabel = isHost ? "玩家1分數: " : "玩家2分數: ";
                String localLifeLabel = isHost ? "玩家1生命: " : "玩家2生命: ";
                String remoteScoreLabel = isHost ? "玩家2分數: " : "玩家1分數: ";
                String remoteLifeLabel = isHost ? "玩家2生命: " : "玩家1生命: ";

                scoreText.setText(localScoreLabel + score);
                lifeText.setText(localLifeLabel + lives);
                remotePlayerScoreText.setText(remoteScoreLabel + remotePlayerScore);
                remotePlayerLifeText.setText(remoteLifeLabel + remotePlayerLives);
            });

            lastReceivedTime = System.currentTimeMillis();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("客戶端: receiveGameState - 接收遊戲狀態時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            handleNetworkError();
        } catch (Exception e) {
            System.err.println("客戶端: receiveGameState - 接收遊戲狀態時發生未知錯誤: " + e.getMessage());
            e.printStackTrace();
            handleNetworkError();
        }
    }

    // 驗證 IP 地址
    private boolean validateIP(String ip) {
        String pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return ip.matches(pattern);
    }

    private void cleanup() {         
        gameOver = true;

        // 停止計時器
        if (timer != null) {
            timer.stop();
            timer = null;
        }

        // 先中斷網路執行緒
        if (networkSyncThread != null) {
            networkSyncThread.interrupt();
            try {
                networkSyncThread.join(500); 
            } catch (InterruptedException e) {
                System.err.println("等待網路執行緒終止時被中斷");
            }
        }

        // 重置變數
        clientWaitingForStartSignal = false;
        clientGameHasStarted = false;
        clientListenerShouldExit = true;
        localEliminated = false;
        remoteEliminated = false;

        // 清空集合
        localObstacles.clear();
        remoteObstacles.clear();
        System.out.println("MultiPlayerPage 資源已清理");
    }

    private void syncRemoteObstacles(ArrayList<NetworkManager.SerializableObstacle> remoteObsList) {
        player2Pane.getChildren().removeAll(remoteObstacles);
        remoteObstacles.clear();

        if (remoteEliminated) {
            return;
        }

        if (remoteObsList != null) {
            for (NetworkManager.SerializableObstacle so : remoteObsList) {
                try {
                    ImageView newObs = so.toImageView();
                    if (newObs != null) {
                        remoteObstacles.add(newObs);
                        player2Pane.getChildren().add(newObs);
                    }
                } catch (Exception e) {
                    System.err.println("轉換遠端障礙物時發生錯誤: " + e.getMessage());
                }
            }
        }
        System.out.println("同步了 " + remoteObstacles.size() + " 個遠端障礙物");
    }
}
