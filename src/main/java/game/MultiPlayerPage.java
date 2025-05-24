package game;

import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.io.IOException;
import java.util.*;

import game.NetworkManager.GameState;
import javafx.application.Platform;
import javafx.scene.control.Alert;

public class MultiPlayerPage {

    private App app;
    private StackPane rootPane;

    // 新增遊戲相關變數
    private Rectangle player;
    private ArrayList<Rectangle> obstacles = new ArrayList<>();
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
    private Rectangle player2;
    private int player2Lane = 1;
    private int player2Score = 0;
    private int player2Lives = 3;
    private Text player2ScoreText, player2LifeText;

    private NetworkManager networkManager = new NetworkManager();
    private boolean isHost;

    public MultiPlayerPage(App app) {
        this.app = app;
    }

    @SuppressWarnings("exports")
    public Parent createContent() {
        rootPane = new StackPane();
        rootPane.setPrefSize(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
        showWaitingPage();
        return rootPane;
    }

    // 顯示等待配對頁面
    private void showWaitingPage() {
        VBox waitingLayout = new VBox(20);
        waitingLayout.setAlignment(Pos.CENTER);

        Button hostButton = new Button("建立遊戲");
        Button joinButton = new Button("加入遊戲");
        TextField ipField = new TextField();
        ipField.setPromptText("輸入主機IP");

        hostButton.setOnAction(e -> {
            try {
                networkManager.createHost(12345);
                showGameContent(true);
            } catch (IOException ex) {
                showError("建立遊戲失敗");
            }
        });

        joinButton.setOnAction(e -> {
            try {
                networkManager.joinGame(ipField.getText(), 12345);
                showGameContent(false);
            } catch (IOException ex) {
                showError("加入遊戲失敗");
            }
        });

        waitingLayout.getChildren().addAll(
                new Label("多人遊戲"),
                hostButton,
                ipField,
                joinButton
        );

        rootPane.getChildren().setAll(waitingLayout);
    }

    // 配對完成後呼叫此方法顯示遊戲內容
    public void showGameContent(boolean isHost) {
        this.isHost = isHost;
        // 初始化遊戲狀態
        gameOver = false;
        speed = 10;
        score = 0;
        lives = 3;
        lastObstacleTime = 0;
        lastUsedLane = -1;
        obstacles.clear();

        // 創建遊戲畫面
        Pane root = new Pane();
        root.setPrefSize(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);

        // 初始化背景
        backgroundCanvas = new RaceTrackCanvas(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
        backgroundCanvas.widthProperty().bind(root.widthProperty());
        backgroundCanvas.heightProperty().bind(root.heightProperty());

        // 初始化遊戲層
        gamePane = new Pane();
        gamePane.prefWidthProperty().bind(root.widthProperty());
        gamePane.prefHeightProperty().bind(root.heightProperty());

        // 創建玩家
        player = new Rectangle(40, 60, Color.BLUE);
        player.layoutYProperty().bind(root.heightProperty().multiply(0.7));

        // 創建分數和生命值顯示
        scoreText = new Text("Score: 0");
        scoreText.layoutXProperty().bind(root.widthProperty().multiply(0.05));
        scoreText.layoutYProperty().bind(root.heightProperty().multiply(0.05));

        lifeText = new Text("Lives: 3");
        lifeText.layoutXProperty().bind(root.widthProperty().multiply(0.05));
        lifeText.layoutYProperty().bind(root.heightProperty().multiply(0.15));

        // 返回按鈕
        Button backButton = new Button("返回首頁");
        UIUtils.applySecondaryButton(backButton);
        backButton.setOnAction(event -> {
            if (timer != null) {
                timer.stop();
            }
            app.switchToHomePage();
        });

        gamePane.getChildren().addAll(player, scoreText, lifeText, backButton);
        root.getChildren().addAll(backgroundCanvas, gamePane);

        rootPane.getChildren().setAll(root);

        // 創建第二位玩家
        player2 = new Rectangle(40, 60, Color.GREEN);
        player2.layoutYProperty().bind(root.heightProperty().multiply(0.7));

        // 創建玩家2的分數和生命值顯示
        player2ScoreText = new Text("Player 2 Score: 0");
        player2ScoreText.layoutXProperty().bind(root.widthProperty().multiply(0.75));
        player2ScoreText.layoutYProperty().bind(root.heightProperty().multiply(0.05));

        player2LifeText = new Text("Player 2 Lives: 3");
        player2LifeText.layoutXProperty().bind(root.widthProperty().multiply(0.75));
        player2LifeText.layoutYProperty().bind(root.heightProperty().multiply(0.15));

        // 將玩家2加入遊戲畫面
        gamePane.getChildren().addAll(player2, player2ScoreText, player2LifeText);

        // 顯示準備開始畫面
        showReadyScreen();
    }

    private void startNetworkSync() {
        new Thread(() -> {
            while (!gameOver) {
                try {
                    // 傳送本地狀態
                    GameState localState = new GameState();
                    localState.playerLane = currentLane;
                    localState.score = score;
                    localState.lives = lives;
                    localState.obstacles = isHost ? obstacles : null; // 只有主機傳送障礙物
                    networkManager.sendGameState(localState);

                    // 接收遠端狀態
                    GameState remoteState = networkManager.receiveGameState();
                    Platform.runLater(() -> {
                        player2Lane = remoteState.playerLane;
                        player2Score = remoteState.score;
                        player2Lives = remoteState.lives;
                        if (!isHost && remoteState.obstacles != null) {
                            // 客戶端同步主機的障礙物
                            syncObstacles(remoteState.obstacles);
                        }
                        updatePlayer2Position();
                        updatePlayer2Score();
                    });

                    Thread.sleep(16);
                } catch (Exception e) {
                    handleNetworkError();
                    break; // 發生錯誤時退出迴圈
                }
            }
        }).start();
    }

    // 新增障礙物同步方法
    private void syncObstacles(ArrayList<Rectangle> hostObstacles) {
        // 清除舊的障礙物
        for (Rectangle obs : obstacles) {
            gamePane.getChildren().remove(obs);
        }
        obstacles.clear();
        
        // 同步新的障礙物
        for (Rectangle hostObs : hostObstacles) {
            Rectangle newObs = new Rectangle(40, 40, Color.RED);
            newObs.setLayoutX(hostObs.getLayoutX());
            newObs.setLayoutY(hostObs.getLayoutY());
            obstacles.add(newObs);
            gamePane.getChildren().add(newObs);
        }
    }

    // 在連線遊戲結束後調用此方法
    private void startGame() {
        Text countdownText = new Text();
        countdownText.setStyle("-fx-font-size: 48px;");
        countdownText.layoutXProperty().bind(
            gamePane.widthProperty().divide(2).subtract(50)
        );
        countdownText.layoutYProperty().bind(
            gamePane.heightProperty().divide(2)
        );
        gamePane.getChildren().add(countdownText);

        if (isHost) {
            // 主機發送開始信號
            try {
                GameState startSignal = new GameState();
                startSignal.gameStarting = true;  // 需要在 GameState 類別中添加此欄位
                networkManager.sendGameState(startSignal);
            } catch (IOException e) {
                handleNetworkError();
                return;
            }
        }

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
                    startNetworkSync();  // 移到這裡開始網路同步
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

        generateObstacles();
        moveObstacles();
        checkCollision();

        // 更新分數顯示
        scoreText.setText("Score: " + score);
    }

    // 修改障礙物生成邏輯，只由主機生成
    private void generateObstacles() {
        if (!isHost) return; // 只有主機生成障礙物

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

            Rectangle obstacle = new Rectangle(40, 40, Color.RED);
            double centerOffset = (gamePane.getWidth() - laneWidth * lanes) / 2.0;
            double obstacleX = lane * laneWidth + centerOffset + (laneWidth - 40) / 2.0;

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
        Iterator<Rectangle> iter = obstacles.iterator();
        while (iter.hasNext()) {
            Rectangle obs = iter.next();
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
        Iterator<Rectangle> iter = obstacles.iterator();
        while (iter.hasNext()) {
            Rectangle obs = iter.next();

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
    private void handleCollision(Rectangle obs, Iterator<Rectangle> iter, boolean isPlayer1) {
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
            gamePane.widthProperty().divide(2).subtract(100)
        );
        gameOverText.layoutYProperty().bind(
            gamePane.heightProperty().divide(2)
        );
        
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
            gamePane.widthProperty().divide(2).subtract(100)
        );
        readyBox.layoutYProperty().bind(
            gamePane.heightProperty().divide(2)
        );
        
        gamePane.getChildren().add(readyBox);
    }
}


