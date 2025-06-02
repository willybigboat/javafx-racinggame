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
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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

    // 常數定義
    private final int WIDTH = laneWidth * lanes + 60;
    private final int HEIGHT = 800;
    private int highScore = 0;

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
        // gamePane = new Pane(); // 移除 gamePane 的初始化
        // gamePane.setPrefSize(WIDTH, HEIGHT); // 移除 gamePane 的尺寸設定
        showWaitingPage(); // 開發完取消註解 !!!
        // showGameContent(true); // 直接顯示遊戲中畫面(開發用)
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
                    // UI 更新必須在 JavaFX 執行緒
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
                new Thread(() -> { // 在新執行緒中執行網路操作
                    try {
                        networkManager.joinGame(ipAddress, 12345);
                        Platform.runLater(() -> {
                            // 先顯示遊戲畫面再顯示準備畫面
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

        // 設置等待框框的位置
        StackPane.setAlignment(waitingLayout, Pos.CENTER);

        rootPane.getChildren().setAll(root);

        // 顯示主機 IP 地址
        showHostIPAddress(waitingLayout);
    }

    // 顯示主機 IP 位址
    private void showHostIPAddress(VBox waitingLayout) {
        try {
            String hostIP = InetAddress.getLocalHost().getHostAddress();
            Text ipText = new Text("你的 IP 地址: " + hostIP);
            ipText.setStyle("-fx-fill: red; -fx-font-size: 16px; -fx-font-weight: bold;");
            // 添加到待機畫面
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
        localObstacles.clear(); // 清除本地障礙物列表
        remoteObstacles.clear(); // 清除遠端障礙物列表

        System.out.println("顯示遊戲畫面，主機: " + isHost); // 診斷印出

        // 創建主畫面容器
        HBox root = new HBox(5);
        root.setPrefSize(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);

        String localCarImage = isHost ? PLAYER1_IMAGE : PLAYER2_IMAGE;
        String remoteCarImage = isHost ? PLAYER2_IMAGE : PLAYER1_IMAGE;

        // 玩家1的遊戲畫面（這是當前玩家）
        player1Pane = createPlayerPane(isHost ? "玩家1" : "玩家2", localCarImage, true);

        // 玩家2的遊戲畫面（對手）
        player2Pane = createPlayerPane(isHost ? "玩家2" : "玩家1", remoteCarImage, false);

        // 將兩個遊戲畫面加入主畫面
        root.getChildren().addAll(player1Pane, player2Pane);

        rootPane.getChildren().clear();
        rootPane.getChildren().add(root);

        System.out.println("遊戲畫面已設置，player1Pane: " + (player1Pane != null) + ", player2Pane: " + (player2Pane != null)); // 診斷印出
        // updatePlayerPosition();
        // updateRemotePlayerPosition();
    }

    private Pane createPlayerPane(String playerName, String carImagePath, boolean isPlayer1) {
        Pane playerPane = new Pane();
        playerPane.setPrefSize(App.WINDOW_WIDTH / 2, App.WINDOW_HEIGHT);

        // 背景
        RaceTrackCanvas backgroundCanvas = new RaceTrackCanvas(App.WINDOW_WIDTH / 2, App.WINDOW_HEIGHT);
        backgroundCanvas.widthProperty().bind(playerPane.widthProperty());
        backgroundCanvas.heightProperty().bind(playerPane.heightProperty());

        // 玩家（用圖片）
        Image carImage = null;
        try {
            carImage = new Image(getClass().getResourceAsStream(carImagePath));
            if (carImage != null && carImage.isError()) { // 檢查是否載入成功
                System.err.println("警告：載入玩家圖片發生錯誤: " + carImagePath);
                carImage = null; // 標記為無效圖片
            }
        } catch (Exception e) {
            System.err.println("錯誤：無法載入玩家圖片: " + carImagePath + " - " + e.getMessage());
            e.printStackTrace();
            carImage = null; // 標記為無效圖片
        }

        // 如果圖片載入失敗，使用預設圖片或佔位符
        if (carImage == null) {
            System.err.println("警告：玩家圖片 " + carImagePath + " 載入失敗。嘗試使用預設紅色方塊。");
            try {
                carImage = new Image(getClass().getResourceAsStream("/image/redBlock.png"));
                if (carImage != null && carImage.isError()) {
                    System.err.println("致命錯誤：載入備用玩家圖片也失敗。");
                    carImage = null; // 最終失敗
                }
            } catch (Exception e) {
                System.err.println("致命錯誤：無法載入備用玩家圖片: " + e.getMessage());
                carImage = null; // 最終失敗
            }
        }

        ImageView playerImg;
        if (carImage != null) {
            playerImg = new ImageView(carImage);
        } else {
            // 如果所有圖片載入都失敗，創建一個空的 ImageView 或一個簡單的佔位符
            System.err.println("嚴重錯誤：無法為玩家創建有效的 ImageView，將使用一個空的 ImageView。");
            playerImg = new ImageView(); // 創建一個空的 ImageView，可能不會顯示任何東西
            // 更好的做法是創建一個 Rectangle 並添加到 Pane 中，但這會改變類型
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
    private Thread networkSyncThread; // 新增成員變數來追蹤網路同步執行緒

    private void startNetworkSync() {
        // 如果先前的同步執行緒存在，先嘗試中斷它
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
                        lastValidReceiveTime = now; // 成功收發後更新此值
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
                    if (!gameOver) { // 只在遊戲未結束時處理網路錯誤
                        handleNetworkError();
                    }
                    break;
                } catch (Exception e) {
                    System.err.println(threadName + " 未知錯誤: " + e.getMessage());
                    e.printStackTrace();
                    if (!gameOver) { // 只在遊戲未結束時處理網路錯誤
                        handleNetworkError();
                    }
                    break;
                }
            }
            System.out.println(threadName + " 執行緒已終止。gameOver = " + gameOver);
        }, "NetworkSyncThread");

        networkSyncThread.setDaemon(true); // 設置為守護執行緒，這樣主程式結束時它也會結束
        networkSyncThread.start();
    }

    // 新增障礙物同步方法 (此方法在客戶端 receiveGameState 中調用)
    private void syncObstacles(ArrayList<NetworkManager.SerializableObstacle> hostObstacles) {
        System.out.println(
                "客戶端: syncObstacles - 開始同步障礙物。收到主機 " + (hostObstacles != null ? hostObstacles.size() : 0) + " 個障礙物。");

        Platform.runLater(() -> {
            // 1. 清除舊的本地障礙物 (從 player1Pane)
            player1Pane.getChildren().removeAll(localObstacles);
            localObstacles.clear();
            System.out.println("客戶端: syncObstacles - 舊本地障礙物已從 player1Pane 移除並清除本地列表。");

            // 2. 清除舊的遠端障礙物 (從 player2Pane)
            player2Pane.getChildren().removeAll(remoteObstacles);
            remoteObstacles.clear();
            System.out.println("客戶端: syncObstacles - 舊遠端障礙物已從 player2Pane 移除並清除遠端列表。");

            // 3. 添加新的障礙物到兩個 Pane
            if (hostObstacles != null) {
                for (NetworkManager.SerializableObstacle so : hostObstacles) {
                    try {
                        // 為本地玩家畫面創建 ImageView
                        ImageView newObsForLocalPlayer = so.toImageView();
                        if (newObsForLocalPlayer != null) {
                            localObstacles.add(newObsForLocalPlayer);
                            player1Pane.getChildren().add(newObsForLocalPlayer);
                        } else {
                            System.err.println("警告：客戶端: syncObstacles - 為本地玩家創建 ImageView 返回 null。");
                        }

                        // 為遠端玩家畫面創建 ImageView (位置需要根據 player2Pane 調整)
                        ImageView newObsForRemotePlayer = so.toImageView(); // 再次調用 toImageView 創建獨立實例
                        if (newObsForRemotePlayer != null) {
                            // 這裡假設 player1Pane 和 player2Pane 的尺寸和座標系統是相同的，
                            // 如果不同，需要根據 player2Pane 的尺寸重新計算 x, y。
                            // 但由於它們是 HBox 中的兩個等寬 Pane，通常直接使用原始座標即可。
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
        // 確保 player1Pane 和 player2Pane 已初始化
        if (player1Pane == null || player2Pane == null) {
            showError("遊戲面板未初始化，無法啟動遊戲。");
            return;
        }

        Text countdownText = new Text();
        countdownText.setStyle("-fx-font-size: 48px;");
        // 倒數計時文字顯示在本地玩家的 Pane 上
        countdownText.layoutXProperty().bind(player1Pane.widthProperty().divide(2).subtract(50));
        countdownText.layoutYProperty().bind(player1Pane.heightProperty().divide(2));
        player1Pane.getChildren().add(countdownText);

        // 雙方確認準備好
        if (isHost) {
            new Thread(() -> { // 主機的阻塞操作也應該在獨立執行緒中
                try {
                    System.out.println("主機：等待客戶端準備好信號...");
                    GameState clientState = networkManager.receiveCompressedGameState(); // 阻塞調用
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
            new Thread(() -> { // 客戶端的阻塞操作也應該在獨立執行緒中
                try {
                    // 客戶端發送準備好信號
                    GameState readySignal = new GameState();
                    readySignal.isReady = true;
                    networkManager.sendCompressedGameState(readySignal);
                    System.out.println("客戶端：已發送準備好信號。");

                    // 等待主機開始信號
                    System.out.println("客戶端：等待主機開始信號...");
                    GameState hostState = networkManager.receiveCompressedGameState(); // 阻塞調用
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
        // 確保計時器停止，防止遊戲提前啟動
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
                    // 從 player1Pane 移除倒數計時文字
                    player1Pane.getChildren().remove(countdownText);

                    // 確保客戶端監聽執行緒已完全停止
                    clientWaitingForStartSignal = false;
                    clientListenerShouldExit = true; // 強制終止監聽循環

                    System.out.println("客戶端: 準備啟動遊戲循環和網路同步...");

                    // 然後啟動遊戲和網路同步
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
        if (player1Pane == null || localPlayer == null) { // 增加 null 檢查
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

    private void updateRemotePlayerPosition() { // 原來的 updatePlayer2Position
        if (player2Pane == null || remotePlayer == null) { // 增加 null 檢查
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

    // 修改 getTrackWidthAtY，讓它可以根據 Pane 高度自動調整
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
        // 如果遊戲已結束，直接返回
        if (gameOver) {
            return;
        }

        // 如果雙方都已淘汰，檢查遊戲結束
        if (localEliminated && remoteEliminated) {
            Platform.runLater(() -> checkGameOver());
            return;
        }

        // 主機端邏輯
        if (isHost) {
            if (localEliminated) {
                // 主機淘汰後，只觀察遠端玩家狀態
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
                // 客戶端淘汰後，只觀察遠端玩家狀態
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
        generateObstacles(); // 主機生成障礙物 (會同時添加到 localObstacles 和 remoteObstacles)
        moveObstacles(); // 移動 localObstacles 和 remoteObstacles
        checkCollision(); // 檢查本地玩家與 localObstacles 的碰撞

        // 更新分數顯示
        // Platform.runLater(() -> scoreText.setText("Score: " + score)); // 確保在 UI
        // 執行緒更新
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
        // 遠端玩家的障礙物和分數由網路同步
        updateRemotePlayerPosition(); // 更新遠端玩家賽車位置
        Platform.runLater(() -> { // 確保在 UI 執行緒更新
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

            // 只為本地玩家畫面創建 ImageView
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
                    continue; // 跳過此障礙物
                }
            }

            obstacle.setImage(obsImg);
            obstacle.setFitWidth(60);
            obstacle.setFitHeight(60);
            obstacle.setUserData(new Object[] { imgPath, lane });

            double obstacleY = -obstacle.getFitHeight();
            double trackWidth = getTrackWidthAtY(obstacleY, player1Pane.getHeight());
            double laneWidthVal = trackWidth / lanes;
            double trackCenterX = player1Pane.getWidth() / 2.0;
            double trackLeftX = trackCenterX - trackWidth / 2.0;
            double obstacleX = trackLeftX + lane * laneWidthVal + (laneWidthVal - obstacle.getFitWidth()) / 2.0;
            obstacle.setLayoutX(obstacleX);
            obstacle.setLayoutY(obstacleY);

            localObstacles.add(obstacle); // 只添加到本地障礙物列表

            final ImageView finalObstacle = obstacle;
            Platform.runLater(() -> player1Pane.getChildren().add(finalObstacle)); // 只添加到本地玩家畫面

            lastUsedLane = lane;
            count++;
            if (count >= obstacleCount) {
                break;
            }
        }
    }

    private void moveObstacles() {
        // 移動本地玩家畫面上的障礙物
        Iterator<ImageView> localIter = localObstacles.iterator();
        while (localIter.hasNext()) {
            ImageView obs = localIter.next();
            double newY = obs.getLayoutY() + speed;
            obs.setLayoutY(newY);

            // 重新計算 X 座標以適應車道變化
            Object userDataContent = obs.getUserData();
            int lane = -1;
            if (userDataContent instanceof Integer) { // 客戶端接收到的 ImageView, userData 是 Integer (lane)
                lane = (Integer) userDataContent;
            } else if (userDataContent instanceof Object[]) { // 主機端的 ImageView, userData 是 Object[]{imgPath, lane}
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

            // 檢查此障礙物是否已加過分（統一使用同樣的屬性標記）
            Boolean scored = (Boolean) obs.getProperties().getOrDefault("scored", false);

            // 如果障礙物超出畫面範圍且還未計分
            if (newY > player1Pane.getHeight() && !scored) {
                // 不論主機或客戶端，都採用相同邏輯：加分、移除障礙物
                score++;
                if (score % 10 == 0) {
                    speed += 1.5;
                }

                // 從UI和列表中移除障礙物（兩端完全相同）
                Platform.runLater(() -> player1Pane.getChildren().remove(obs));
                localIter.remove();
            }
        }

        // 移動遠端玩家畫面上的障礙物
        Iterator<ImageView> remoteIter = remoteObstacles.iterator();
        while (remoteIter.hasNext()) {
            ImageView obs = remoteIter.next();
            double newY = obs.getLayoutY() + speed; // 遠端障礙物也以相同速度移動
            obs.setLayoutY(newY);

            // 重新計算 X 座標以適應車道變化
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
                double trackWidth = getTrackWidthAtY(newY, player2Pane.getHeight()); // 使用 player2Pane 的高度
                double laneWidthVal = trackWidth / lanes;
                double trackCenterX = player2Pane.getWidth() / 2.0;
                double trackLeftX = trackCenterX - trackWidth / 2.0;
                double obstacleX = trackLeftX + lane * laneWidthVal + (laneWidthVal - obs.getFitWidth()) / 2.0;
                obs.setLayoutX(obstacleX);
            }

            // 遠端障礙物移出畫面時從UI和列表中移除（兩端完全相同）
            if (newY > player2Pane.getHeight()) {
                Platform.runLater(() -> player2Pane.getChildren().remove(obs));
                remoteIter.remove();
            }
        }
    }

    // 修改碰撞檢查，只針對本地玩家
    private void checkCollision() {
        Iterator<ImageView> iter = localObstacles.iterator(); // 只檢查本地障礙物
        while (iter.hasNext()) {
            ImageView obs = iter.next();

            // 檢查本地玩家 (localPlayer) 碰撞
            if (localPlayer != null && obs != null
                    && localPlayer.getBoundsInParent().intersects(obs.getBoundsInParent())) {
                handleCollision(obs, iter, true); // isLocalPlayer = true
            }
            // 遠端玩家的碰撞由他們自己的機器處理，不在此處檢查
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

    // 2. 本地玩家淘汰時顯示等待畫面
    private void onLocalEliminated() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        Platform.runLater(() -> {
            // 顯示淘汰訊息
            Text eliminatedText = new Text("你已淘汰，等待其他玩家結束...");
            eliminatedText.setStyle("-fx-font-size: 28px; -fx-fill: orange; -fx-stroke: black; -fx-stroke-width: 1;");
            eliminatedText.setLayoutX(player1Pane.getWidth() / 2 - 150);
            eliminatedText.setLayoutY(player1Pane.getHeight() / 2);
            player1Pane.getChildren().add(eliminatedText);

            // 重要：當玩家被淘汰時，清除自己畫面上的障礙物
            player1Pane.getChildren().removeAll(localObstacles);
            localObstacles.clear();

            // 確保在 UI 執行緒中立即檢查遊戲結束條件
            checkGameOver();
        });

        // 建立一個新的 Timer 來處理對手畫面更新（只同步對手畫面，不處理自己畫面）
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

    // 新增方法：只更新遠端玩家
    private void updateRemotePlayerOnly() {
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

            // 更新遠端玩家位置
            updateRemotePlayerPosition();
        });
    }

    private void onRemoteEliminated() {
        Platform.runLater(() -> {
            Text infoText = new Text("對方已淘汰，你可以繼續挑戰高分！");
            infoText.setStyle("-fx-font-size: 20px; -fx-fill: green; -fx-stroke: black; -fx-stroke-width: 1;");
            infoText.setLayoutX(player1Pane.getWidth() / 2 - 120);
            infoText.setLayoutY(40);
            player1Pane.getChildren().add(infoText);

            // 重要：當遠端玩家被淘汰時，清除遠端畫面上的障礙物
            player2Pane.getChildren().removeAll(remoteObstacles);
            remoteObstacles.clear();

            // 確保在 UI 執行緒中立即檢查遊戲結束條件
            checkGameOver();
        });
    }

    // 4. 雙方都淘汰時跳轉到 mulGameOverPage
    private void checkGameOver() {
        System.out.println("[CheckGameOver] localEliminated=" + localEliminated + ", remoteEliminated="
                + remoteEliminated + ", gameOver=" + gameOver);

        if ((localEliminated && remoteEliminated) || gameOver) {
            if (!gameOver) {
                gameOver = true;
                System.out.println("[CheckGameOver] 雙方均已淘汰，設置 gameOver = true");

                // 確保最後一次傳送遊戲結束標記
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

            // 清理資源，但不關閉網路連線
            cleanup();

            // 確保已非同步處理任何網路資源
            Platform.runLater(() -> {
                try {
                    Thread.sleep(200); // 給網路執行緒一些時間來處理最後的訊息
                } catch (InterruptedException e) {
                    // 忽略中斷
                }

                // 跳轉到結算畫面
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

        Platform.runLater(() -> { // 確保在 UI 執行緒更新
            // 顯示遊戲結束訊息
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

            // 使用固定位置而非綁定
            double textX = player1Pane.getWidth() / 2 - 100; // 顯示在本地玩家畫面中央
            double textY = player1Pane.getHeight() / 2;
            gameOverText.setLayoutX(textX);
            gameOverText.setLayoutY(textY);

            player1Pane.getChildren().add(gameOverText);

            // 添加返回按鈕，避免直接斷線
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

        // 如果遊戲已結束，不再處理網路錯誤（避免誤報）
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
        // 確保 player1Pane 已初始化
        if (player1Pane == null) {
            showError("遊戲畫面未初始化 (player1Pane is null)");
            return;
        }

        // 如果先前的 readyBox 存在，則移除
        if (activeReadyBox != null && player1Pane.getChildren().contains(activeReadyBox)) {
            player1Pane.getChildren().remove(activeReadyBox);
        }
        clientGameHasStarted = false; // 顯示準備畫面時重置旗標

        VBox newReadyBox = new VBox(20);
        newReadyBox.setAlignment(Pos.CENTER);
        newReadyBox.setStyle(
                "-fx-background-color: rgba(255,255,255,0.8); -fx-padding: 20px; -fx-border-radius: 15; -fx-background-radius: 15;");

        Text waitingText = new Text(isHost ? "等待玩家加入..." : "等待主機開始...");
        waitingText.setStyle("-fx-font-size: 24px; -fx-fill: #333;");

        Button readyButton = new Button("準備開始");
        readyButton.setStyle("-fx-font-size: 18px;");
        readyButton.setVisible(isHost); // 僅對主機可見

        this.activeReadyBox = newReadyBox; // 儲存新的 readyBox 實例

        if (isHost) {
            readyButton.setOnAction(e -> {
                Platform.runLater(() -> { // 確保 UI 移除在 UI 執行緒
                    if (activeReadyBox != null && player1Pane.getChildren().contains(activeReadyBox)) {
                        player1Pane.getChildren().remove(activeReadyBox);
                        activeReadyBox = null; // 清除引用
                    }
                });
                // 主機發送開始信號並啟動自己的倒計時
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
                            startCountdown(countdownText); // 主機開始倒計時
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
            clientListenerShouldExit = false; // 重置終止標誌

            new Thread(() -> {
                System.out.println("客戶端: 開始監聽主機開始信號");
                try {
                    while (clientWaitingForStartSignal && !gameOver && !clientGameHasStarted
                            && !clientListenerShouldExit) {
                        System.out.println("客戶端: 等待主機開始信號...");
                        GameState hostState = networkManager.receiveCompressedGameState(); // 阻塞調用
                        System.out.println("客戶端: 收到遊戲狀態，gameStarting = " + hostState.gameStarting);

                        if (hostState != null && hostState.gameStarting) {
                            if (clientWaitingForStartSignal && !clientGameHasStarted && !clientListenerShouldExit) { // 再次檢查
                                clientWaitingForStartSignal = false; // 停止此監聽器
                                clientListenerShouldExit = true; // 強制終止監聽循環
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
                                        startCountdown(countdownText); // 客戶端開始倒數
                                    } else {
                                        System.err.println("客戶端: 遊戲面板未準備就緒");
                                        showError("客戶端錯誤：遊戲面板未準備就緒。");
                                    }
                                });
                            }
                            break; // 退出監聽循環
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    if (!clientListenerShouldExit) { // 只有在不是因為主動終止而導致的錯誤才處理
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
                    clientWaitingForStartSignal = false; // 確保旗標被重置
                }
            }, "ClientStartSignalListener").start();
        }

        activeReadyBox.getChildren().addAll(waitingText, readyButton);

        double boxWidth = 200;
        double boxHeight = 100;
        activeReadyBox.setLayoutX((player1Pane.getPrefWidth() - boxWidth) / 2); // 顯示在本地玩家畫面中央
        activeReadyBox.setLayoutY((player1Pane.getPrefHeight() - boxHeight) / 2);

        player1Pane.getChildren().add(activeReadyBox); // 添加到本地玩家畫面
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
                    imgPath = "/image/redBlock.png"; // 預設圖片
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

            // 重要！增加遊戲結束狀態標記
            if (localEliminated && remoteEliminated) {
                localState.gameEnded = true; // 雙方都結束時，發送結束標記
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

            // 如果收到遊戲結束標記，立即檢查遊戲結束
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

                    // 立即檢查遊戲結束
                    if (localEliminated) {
                        Platform.runLater(() -> checkGameOver());
                    }
                }
                Platform.runLater(() -> updateRemotePlayerPosition());
            } else {
                if (remoteState.lives <= 0 && !remoteEliminated) {
                    remoteEliminated = true;
                    onRemoteEliminated();

                    // 立即檢查遊戲結束
                    if (localEliminated) {
                        Platform.runLater(() -> checkGameOver());
                    }
                }
                if (lives <= 0 && !localEliminated) {
                    localEliminated = true;
                    onLocalEliminated();

                    // 立即檢查遊戲結束
                    if (remoteEliminated) {
                        Platform.runLater(() -> checkGameOver());
                    }
                }
                Platform.runLater(() -> updatePlayerPosition());

                // 同步主機玩家位置
                remotePlayerLane = remoteState.playerLane;
                remotePlayerScore = remoteState.score;
                remotePlayerLives = remoteState.lives;
                // 即使客戶端淘汰了，仍然同步對手的障礙物
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
        // 設置遊戲結束標誌，讓所有執行緒知道該停止了
        gameOver = true;

        // 停止計時器
        if (timer != null) {
            timer.stop();
            timer = null;
        }

        // 先中斷網路執行緒
        if (networkSyncThread != null) {
            networkSyncThread.interrupt();
            // 給執行緒一些時間來終止
            try {
                networkSyncThread.join(500); // 最多等待500ms
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

        // 注意：不在這裡關閉網路連線，讓 mulGameOverPage 在用戶點擊"返回首頁"時關閉

        System.out.println("MultiPlayerPage 資源已清理");
    }

    private void syncRemoteObstacles(ArrayList<NetworkManager.SerializableObstacle> remoteObsList) {
        // 清除舊的遠端障礙物
        player2Pane.getChildren().removeAll(remoteObstacles);
        remoteObstacles.clear();

        // 如果對手已淘汰，不顯示任何障礙物
        if (remoteEliminated) {
            return;
        }

        // 添加新的遠端障礙物 (只有對手未淘汰時才添加)
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
