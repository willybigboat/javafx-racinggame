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

    // 新增遊戲相關變數
    private ImageView player;
    private ArrayList<ImageView> obstacles = new ArrayList<>();
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
    private Pane gamePane;
    private RaceTrackCanvas backgroundCanvas;

    // 新增常數定義
    private final int WIDTH = laneWidth * lanes + 60;
    private final int HEIGHT = 800;
    private int highScore = 0;

    // 新增玩家2的變數
    private ImageView player2;
    private int player2Lane = 1;
    private int player2Score = 0;
    private int player2Lives = 3;
    private Text player2ScoreText, player2LifeText;

    private NetworkManager networkManager = new NetworkManager();
    private boolean isHost;

    // 新增網路同步相關變數
    private long lastNetworkSync = 0;
    private static final long NETWORK_SYNC_INTERVAL = 16; // ms
    private static final long NETWORK_TIMEOUT = 5000; // 5秒逾時
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

    public MultiPlayerPage(App app) {
        this.app = app;
    }

    @SuppressWarnings("exports")
    public Parent createContent() {
        rootPane = new StackPane();
        rootPane.setPrefSize(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
        gamePane = new Pane();
        gamePane.setPrefSize(WIDTH, HEIGHT);
        //showWaitingPage(); 開發完取消註解 !!!
        showGameContent(true); // 直接顯示遊戲中畫面(開發用)
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
        waitingLayout.setMinWidth(App.WINDOW_WIDTH/2);
        waitingLayout.setMaxWidth(App.WINDOW_WIDTH/2);
        waitingLayout.setMinHeight(App.WINDOW_HEIGHT/2.5);
        waitingLayout.setMaxHeight(App.WINDOW_HEIGHT/2.5);
        waitingLayout.setAlignment(Pos.CENTER);
        waitingLayout.setPrefSize(App.WINDOW_WIDTH/2, App.WINDOW_HEIGHT/2.5);
        waitingLayout.setStyle("-fx-background-color: rgba(255, 255, 255, 0.7); -fx-padding: 20px; -fx-border-radius: 10px; -fx-background-radius: 10px;");

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
                    Platform.runLater(() -> showGameContent(true));
                } catch (IOException ex) {
                    Platform.runLater(() -> showError("建立遊戲失敗"));
                }
            }).start();
        });

        // 驗證 IP 地址
        joinButton.setOnAction(e -> {
            String ipAddress = ipField.getText();
            if (validateIP(ipAddress)) {
                try {
                    networkManager.joinGame(ipAddress, 12345);
                    showGameContent(false);
                } catch (IOException ex) {
                    showError("加入遊戲失敗: " + ex.getMessage());
                }
            } else {
                showError("IP 地址格式無效");
            }
        });
        joinLayout.getChildren().addAll(ipField, joinButton);
        btHbox.getChildren().addAll(hostButton, joinLayout);

        waitingLayout.getChildren().addAll(
                btHbox,
                backButton
        );

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
        obstacles.clear();

        // 創建主畫面容器
        HBox root = new HBox(5);
        root.setPrefSize(App.WINDOW_WIDTH*2, App.WINDOW_HEIGHT);

        // 玩家1的遊戲畫面
        Pane player1Pane = createPlayerPane("玩家1", PLAYER1_IMAGE, true);
        // 玩家2的遊戲畫面
        Pane player2Pane = createPlayerPane("玩家2", PLAYER2_IMAGE, false);

        // 將兩個遊戲畫面加入主畫面
        root.getChildren().addAll(player1Pane, player2Pane);

        rootPane.getChildren().setAll(root);

        // 顯示準備開始畫面
        showReadyScreen();
    }

    private Pane createPlayerPane(String playerName, String carImagePath, boolean isPlayer1) {
        Pane playerPane = new Pane();
        playerPane.setPrefSize(App.WINDOW_WIDTH, App.WINDOW_HEIGHT / 2);

        // 背景
        RaceTrackCanvas backgroundCanvas = new RaceTrackCanvas(App.WINDOW_WIDTH/2, App.WINDOW_HEIGHT);
        backgroundCanvas.widthProperty().bind(playerPane.widthProperty());
        backgroundCanvas.heightProperty().bind(playerPane.heightProperty());

        // 玩家（用圖片）
        Image carImage = new Image(getClass().getResourceAsStream(carImagePath));
        ImageView playerImg = new ImageView(carImage);
        playerImg.setFitWidth(60);
        playerImg.setFitHeight(90);
        playerImg.setLayoutY(playerPane.getPrefHeight() * 0.7);

        // 玩家資訊
        Text playerText = new Text(playerName);
        playerText.setStyle("-fx-font-size: 18px;");
        Text scoreText = new Text(" Score: 0");
        scoreText.setStyle("-fx-font-size: 16px;");
        Text lifeText = new Text(" Lives: 3");
        lifeText.setStyle("-fx-font-size: 16px;");

        VBox infoBox = new VBox(8, playerText, scoreText, lifeText);
        infoBox.setAlignment(Pos.TOP_LEFT);
        infoBox.setPadding(new Insets(10));
        infoBox.setStyle("-fx-background-color: rgba(255,255,255,0.7); -fx-background-radius: 10px;");
        infoBox.setLayoutX(15);
        infoBox.setLayoutY(15);

        if (isPlayer1) {
            this.player = playerImg;
            this.scoreText = scoreText;
            this.lifeText = lifeText;
        } else {
            this.player2 = playerImg;
            this.player2ScoreText = scoreText;
            this.player2LifeText = lifeText;
        }

        playerPane.getChildren().addAll(backgroundCanvas, playerImg, infoBox);
        return playerPane;
    }

    private void startNetworkSync() {
        new Thread(() -> {
            while (!gameOver) {
                try {
                    long now = System.currentTimeMillis();

                    // 控制同步頻率
                    if (now - lastNetworkSync >= NETWORK_SYNC_INTERVAL) {
                        sendGameState();
                        receiveGameState();
                        lastNetworkSync = now;
                    }

                    // 檢查是否長時間沒收到對方資訊
                    if (now - lastReceivedTime > NETWORK_TIMEOUT) {
                        throw new IOException("連線逾時");
                    }

                    Thread.sleep(5); // 釋放CPU
                } catch (Exception e) {
                    handleNetworkError();
                    break;
                }
            }
        }).start();
    }

    // 新增障礙物同步方法
    private void syncObstacles(ArrayList<NetworkManager.SerializableObstacle> hostObstacles) {
        // 清除舊的障礙物
        for (ImageView obs : obstacles) {
            gamePane.getChildren().remove(obs);
        }
        obstacles.clear();

        for (NetworkManager.SerializableObstacle so : hostObstacles) {
            ImageView newObs = so.toImageView();
            obstacles.add(newObs);
            gamePane.getChildren().add(newObs);
        }
    }

    // 在連線遊戲結束後調用此方法
    private void startGame() {
        Text countdownText = new Text();
        countdownText.setStyle("-fx-font-size: 48px;");
        countdownText.layoutXProperty().bind(gamePane.widthProperty().divide(2).subtract(50));
        countdownText.layoutYProperty().bind(gamePane.heightProperty().divide(2));
        gamePane.getChildren().add(countdownText);

        // 雙方確認準備好
        if (isHost) {
            try {
                // 主機等待客戶端準備好
                GameState clientState = networkManager.receiveGameState();
                if (clientState.isReady) {
                    // 發送開始倒計時信號
                    GameState startSignal = new GameState();
                    startSignal.gameStarting = true;
                    networkManager.sendGameState(startSignal);
                    startCountdown(countdownText);
                }
            } catch (Exception e) {
                handleNetworkError();
            }
        } else {
            try {
                // 客戶端發送準備好信號
                GameState readySignal = new GameState();
                readySignal.isReady = true;
                networkManager.sendGameState(readySignal);

                // 等待主機開始信號
                GameState hostState = networkManager.receiveGameState();
                if (hostState.gameStarting) {
                    startCountdown(countdownText);
                }
            } catch (Exception e) {
                handleNetworkError();
            }
        }
    }

    private void startCountdown(Text countdownText) {
        // 開始倒數
        new Thread(() -> {
            try {
                for (int i = 3; i > 0; i--) {
                    final int count = i;
                    Platform.runLater(() -> countdownText.setText(String.valueOf(count)));
                    Thread.sleep(1000);
                }
                Platform.runLater(() -> {
                    countdownText.setText("開始！");
                    gamePane.getChildren().remove(countdownText);
                    startGameLoop();
                    startNetworkSync(); // 移到這裡開始網路同步
                });
                Thread.sleep(1000);
                Platform.runLater(() -> gamePane.getChildren().remove(countdownText));
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
        double centerOffset = (gamePane.getWidth() - laneWidth * lanes) / 2.0;
        double playerX = currentLane * laneWidth + centerOffset + (laneWidth - 40) / 2.0;
        player.setLayoutX(playerX);
    }

    // 新增玩家2位置更新方法
    private void updatePlayer2Position() {
        double centerOffset = (gamePane.getWidth() - laneWidth * lanes) / 2.0;
        double playerX = player2Lane * laneWidth + centerOffset + (laneWidth - 40) / 2.0;
        player2.setLayoutX(playerX);
    }

    private void update() {
        if (gameOver) {
            return;
        }

        // 更新玩家1的畫面
        updatePlayer1();
        // 更新玩家2的畫面
        updatePlayer2();
    }

    // 更新玩家1的畫面
    private void updatePlayer1() {
        generateObstacles();
        moveObstacles();
        checkCollision();

        // 更新分數顯示
        scoreText.setText("Score: " + score);
    }

    // 更新玩家2的畫面
    private void updatePlayer2() {
        // 玩家2的障礙物和分數由網路同步
        updatePlayer2Position();
        player2ScoreText.setText("Player 2 Score: " + player2Score);
        player2LifeText.setText("Player 2 Lives: " + player2Lives);
    }

    // 修改障礙物生成邏輯，只由主機生成
    private void generateObstacles() {
        if (!isHost)
            return; // 只有主機生成障礙物

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

             // 隨機選擇障礙物圖片
            String imgPath = OBSTACLE_IMAGES[rand.nextInt(OBSTACLE_IMAGES.length)];
            Image obsImg = new Image(getClass().getResourceAsStream(imgPath));
            ImageView obstacle = new ImageView(obsImg);
            obstacle.setFitWidth(60);
            obstacle.setFitHeight(60);
            obstacle.setUserData(imgPath); // 關鍵：記錄圖片路徑

            double centerOffset = (gamePane.getWidth() - laneWidth * lanes) / 2.0;
            double laneStartX = lane * laneWidth + centerOffset;
            double obstacleX = laneStartX + (laneWidth - obstacle.getFitWidth()) / 2.0;
            obstacle.setLayoutX(obstacleX);
            obstacle.setLayoutY(0);
            obstacles.add(obstacle);
            gamePane.getChildren().add(obstacle);

            lastUsedLane = lane;
            count++;
            if (count >= obstacleCount) {
                break;
            }
        }
    }

    private void moveObstacles() {
        Iterator<ImageView> iter = obstacles.iterator();
        while (iter.hasNext()) {
            ImageView obs = iter.next();
            obs.setLayoutY(obs.getLayoutY() + speed);

            if (obs.getLayoutY() > HEIGHT) {
                gamePane.getChildren().remove(obs);
                iter.remove();
                score++;
                scoreText.setText("Score: " + score);
                if (score % 10 == 0) {
                    speed += 1.5;
                }
            }
        }
    }

    // 修改碰撞檢查，包含兩位玩家
    private void checkCollision() {
        Iterator<ImageView> iter = obstacles.iterator();
        while (iter.hasNext()) {
            ImageView obs = iter.next();

            // 檢查玩家1碰撞
            if (player.getBoundsInParent().intersects(obs.getBoundsInParent())) {
                handleCollision(obs, iter, true);
            }

            // 檢查玩家2碰撞
            if (player2.getBoundsInParent().intersects(obs.getBoundsInParent())) {
                handleCollision(obs, iter, false);
            }
        }
    }

    // 新增碰撞處理方法
    private void handleCollision(ImageView obs, Iterator<ImageView> iter, boolean isPlayer1) {
        gamePane.getChildren().remove(obs);
        iter.remove();

        if (isPlayer1) {
            lives--;
            lifeText.setText("Lives: " + lives);
            if (lives <= 0) {
                checkGameOver();
            }
        } else {
            player2Lives--;
            player2LifeText.setText("Player 2 Lives: " + player2Lives);
            if (player2Lives <= 0) {
                checkGameOver();
            }
        }

        speed = Math.max(10, speed - 1);
    }

    // 修改遊戲結束檢查
    private void checkGameOver() {
        if (lives <= 0 && player2Lives <= 0) {
            endGame();
        }
    }

    // 修改遊戲結束方法
    private void endGame() {
        gameOver = true;

        // 計算最終分數並更新最高分
        int finalScore = Math.max(score, player2Score);
        if (finalScore > highScore) {
            highScore = finalScore;
        }

        if (timer != null) {
            timer.stop();
        }

        // 顯示遊戲結束訊息
        Text gameOverText = new Text();
        gameOverText.setStyle("-fx-font-size: 24px;");

        // 判斷勝負
        String message;
        if (score > player2Score) {
            message = "玩家1 獲勝! 分數: " + score;
        } else if (player2Score > score) {
            message = "玩家2 獲勝! 分數: " + player2Score;
        } else {
            message = "平手! 分數: " + score;
        }

        gameOverText.setText(message);
        gameOverText.layoutXProperty().bind(
                gamePane.widthProperty().divide(2).subtract(100));
        gameOverText.layoutYProperty().bind(
                gamePane.heightProperty().divide(2));

        gamePane.getChildren().add(gameOverText);

        // 關閉網路連線
        try {
            networkManager.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 3秒後返回主選單
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> app.switchToHomePage());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updatePlayer2Score() {
        player2ScoreText.setText("Player 2 Score: " + player2Score);
        player2LifeText.setText("Player 2 Lives: " + player2Lives);
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
        showError("網路連線中斷");
        gameOver = true;
        if (timer != null) {
            timer.stop();
        }
        Platform.runLater(() -> app.switchToHomePage());
    }

    private void showReadyScreen() {
        VBox readyBox = new VBox(20);
        readyBox.setAlignment(Pos.CENTER);

        Text waitingText = new Text(isHost ? "等待玩家加入..." : "等待主機開始...");
        waitingText.setStyle("-fx-font-size: 24px;");

        Button readyButton = new Button("準備開始");
        readyButton.setVisible(isHost);

        readyButton.setOnAction(e -> {
            readyBox.getChildren().clear();
            startGame();
        });

        readyBox.getChildren().addAll(waitingText, readyButton);
        readyBox.layoutXProperty().bind(
                gamePane.widthProperty().divide(2).subtract(100));
        readyBox.layoutYProperty().bind(
                gamePane.heightProperty().divide(2));

        gamePane.getChildren().add(readyBox);
    }

    // 修改障礙物發送邏輯
    private void sendGameState() {
        try {
            GameState localState = new GameState();
            localState.playerLane = currentLane;
            localState.score = score;
            localState.lives = lives;

            // 轉換障礙物為可序列化格式
            if (isHost && !obstacles.isEmpty()) {
                ArrayList<NetworkManager.SerializableObstacle> serializableObstacles = new ArrayList<>();
                for (ImageView obs : obstacles) {
                    serializableObstacles.add(new NetworkManager.SerializableObstacle(obs));
                }
                localState.obstacles = serializableObstacles;
            }

            networkManager.sendGameState(localState);
        } catch (IOException e) {
            handleNetworkError();
        }
    }

    // 修改障礙物接收邏輯
    private void receiveGameState() {
        try {
            GameState remoteState = networkManager.receiveGameState();

            // 更新玩家狀態
            if (remoteState.playerLane != currentLane) {
                currentLane = remoteState.playerLane;
                updatePlayerPosition();
            }
            score = remoteState.score;
            lives = remoteState.lives;

            // 同步障礙物
            if (isHost) {
                syncObstacles(remoteState.obstacles);
            } else {
                if (remoteState.obstacles != null) {
                    obstacles.clear();
                    for (NetworkManager.SerializableObstacle so : remoteState.obstacles) {
                        ImageView obs = so.toImageView();
                        obstacles.add(obs);
                        gamePane.getChildren().add(obs);
                    }
                }
            }

            // 更新分數和生命顯示
            scoreText.setText("Score: " + score);
            lifeText.setText("Lives: " + lives);
            player2ScoreText.setText("Player 2 Score: " + player2Score);
            player2LifeText.setText("Player 2 Lives: " + player2Lives);

            lastReceivedTime = System.currentTimeMillis();
        } catch (IOException | ClassNotFoundException e) {
            handleNetworkError();
        }
    }

    // 驗證 IP 地址
    private boolean validateIP(String ip) {
        String pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return ip.matches(pattern);
    }
}
