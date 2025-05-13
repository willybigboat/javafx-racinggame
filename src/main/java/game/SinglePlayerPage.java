package game;

import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.util.*;

public class SinglePlayerPage {

    private App app;
    private Rectangle player;
    private ArrayList<Rectangle> obstacles = new ArrayList<>();
    private int currentLane = 1;
    private final int lanes = 4;
    private final int laneWidth = 70;

    private double speed;
    private int score;
    private int highScore = 0;
    private int lives;
    private boolean gameOver;

    private long lastObstacleTime;
    private int lastUsedLane;

    private Text scoreText, lifeText, highScoreText, gameOverText;
    private Random rand = new Random();

    private final int WIDTH = laneWidth * lanes + 60;
    private final int HEIGHT = 800;

    private AnimationTimer timer;
    private Runnable onGameOverCallback;

    private Pane gamePane;
    private RaceTrackCanvas backgroundCanvas;

    public SinglePlayerPage(App app) {
        this.app = app;
        // 重置遊戲狀態
        score = 0;
        gameOver = false;
        obstacles.clear();  // 如果你使用了 ArrayList 來存儲障礙物
    }

    @SuppressWarnings("exports")
    public Parent createContent() {
        // 停止舊計時器（如果存在）
        if (timer != null) {
            timer.stop();
            timer = null;
        }

        // 重置遊戲狀態
        gameOver = false;
        speed = 10;
        score = 0;
        lives = 3;
        lastObstacleTime = 0;
        lastUsedLane = -1;
        obstacles.clear();

        VBox uiBox = new VBox(20);
        uiBox.setAlignment(Pos.TOP_CENTER);

        Button backButton = new Button("返回首頁");
        backButton.setStyle("-fx-font-size: 20px;");
        backButton.setOnAction(event -> {
            if (timer != null) timer.stop();
            app.switchToHomePage();
        });

        // 創建主要的 Pane
        Pane root = new Pane();
         root.setPrefSize(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
        
        // 綁定背景畫布的大小
        backgroundCanvas = new RaceTrackCanvas(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
        backgroundCanvas.widthProperty().bind(root.widthProperty());
        backgroundCanvas.heightProperty().bind(root.heightProperty());

        // 綁定遊戲層的大小
        gamePane = new Pane();
        gamePane.prefWidthProperty().bind(root.widthProperty());
        gamePane.prefHeightProperty().bind(root.heightProperty());

        // 玩家位置綁定
        player = new Rectangle(40, 60, Color.BLUE);
        player.layoutYProperty().bind(root.heightProperty().multiply(0.7)); // 設置在畫面70%的位置

        // 分數文字位置綁定
        scoreText = new Text("Score: 0");
        scoreText.layoutXProperty().bind(root.widthProperty().multiply(0.05));
        scoreText.layoutYProperty().bind(root.heightProperty().multiply(0.05));

        highScoreText = new Text("High Score: " + highScore);
        highScoreText.layoutXProperty().bind(root.widthProperty().multiply(0.05));
        highScoreText.layoutYProperty().bind(root.heightProperty().multiply(0.1));

        lifeText = new Text("Lives: 3");
        lifeText.layoutXProperty().bind(root.widthProperty().multiply(0.05));
        lifeText.layoutYProperty().bind(root.heightProperty().multiply(0.15));

        gameOverText = new Text();
        gameOverText.layoutXProperty().bind(
            root.widthProperty().divide(2).subtract(70)
        );
        gameOverText.layoutYProperty().bind(
            root.heightProperty().multiply(0.4)
        );

        gamePane.getChildren().clear(); // 清空畫面
        gamePane.getChildren().addAll(player, scoreText, highScoreText, lifeText, gameOverText);

        // 停止舊計時器（如果存在）
        if (timer != null) timer.stop();

        root.getChildren().add(uiBox);
        root.getChildren().addAll(backgroundCanvas, gamePane);

        return root;
    }

    // 新增啟動遊戲的方法
    public void startGame() {
        gameOver = false;
        startGameLoop();
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
        if (gameOver) return;
        KeyCode code = event.getCode();
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

    private void generateObstacles() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastObstacleTime < 1000) return;
        lastObstacleTime = currentTime;

        ArrayList<Integer> laneIndices = new ArrayList<>();
        for (int i = 0; i < lanes; i++) laneIndices.add(i);
        Collections.shuffle(laneIndices);

        int obstacleCount = 1 + rand.nextInt(2);
        int count = 0;

        for (int lane : laneIndices) {
            if (lane == lastUsedLane && laneIndices.size() > 1) continue;

            Rectangle obstacle = new Rectangle(40, 40, Color.RED);
            double centerOffset = (gamePane.getWidth() - laneWidth * lanes) / 2.0;
            double obstacleX = lane * laneWidth + centerOffset + (laneWidth - 40) / 2.0;

            obstacle.setLayoutX(obstacleX);
            obstacle.setLayoutY(0);
            obstacles.add(obstacle);
            gamePane.getChildren().add(obstacle);

            lastUsedLane = lane;
            count++;
            if (count >= obstacleCount) break;
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

    private void checkCollision() {
        Iterator<Rectangle> iter = obstacles.iterator();
        while (iter.hasNext()) {
            Rectangle obs = iter.next();
            if (player.getBoundsInParent().intersects(obs.getBoundsInParent())) {
                gamePane.getChildren().remove(obs);
                iter.remove();
                lives--;
                speed = Math.max(10, speed - 1);
                lifeText.setText("Lives: " + lives);
                if (lives <= 0) {
                    endGame();
                }
                break;
            }
        }
    }

    private void update() {
        if (gameOver) return;
        
        generateObstacles();
        moveObstacles();
        checkCollision();
        
        // 更新分數顯示
        scoreText.setText("Score: " + score);
        highScoreText.setText("High Score: " + highScore);
    }

    private void endGame() {
        gameOver = true;
        if (score > highScore) {
            highScore = score;
        }
        
        // 停止遊戲計時器
        if (timer != null) {
            timer.stop();
        }
        
        // 切換到遊戲結束頁面
        app.switchToGameOver();
    }

    public int getScore() {
        return score;
    }

    public int getHighScore() {
        return highScore;
    }
}
