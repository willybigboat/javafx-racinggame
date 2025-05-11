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

        backgroundCanvas = new RaceTrackCanvas(WIDTH, HEIGHT);
        gamePane = new Pane();
        gamePane.setPrefSize(WIDTH, HEIGHT);

        StackPane root = new StackPane();
        root.setPrefSize(WIDTH, HEIGHT);
        root.getChildren().addAll(backgroundCanvas, gamePane);

        // 修改焦點和事件處理設定
        root.setFocusTraversable(true);
        root.requestFocus();  // 確保根節點獲得焦點

        // 將鍵盤事件處理器直接加到根節點
        root.setOnKeyPressed(this::handleKeyPress);

        player = new Rectangle(40, 60, Color.BLUE);
        updatePlayerPosition();

        scoreText = new Text("Score: 0");
        scoreText.setLayoutX(10);
        scoreText.setLayoutY(20);

        highScoreText = new Text("High Score: " + highScore);
        highScoreText.setLayoutX(10);
        highScoreText.setLayoutY(40);

        lifeText = new Text("Lives: 3");
        lifeText.setLayoutX(10);
        lifeText.setLayoutY(60);

        gameOverText = new Text();
        gameOverText.setLayoutX(WIDTH / 2.0 - 70);
        gameOverText.setLayoutY(300);
        gameOverText.setFill(Color.RED);
        gameOverText.setStyle("-fx-font-size: 24px;");

        gamePane.getChildren().clear(); // 清空畫面
        gamePane.getChildren().addAll(player, scoreText, highScoreText, lifeText, gameOverText);

        // 停止舊計時器（如果存在）
        if (timer != null) {
            timer.stop();
        }

        startGameLoop();
        root.getChildren().add(uiBox);

        return root;
    }

    private void startGameLoop() {
        if (timer != null) {
            timer.stop();
        }

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
            }
        };
        timer.start();
    }

    public void handleKeyPress(KeyEvent event) {
        if (gameOver) {
            return;
        }
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
        double centerOffset = (WIDTH - laneWidth * lanes) / 2.0;
        player.setLayoutX(currentLane * laneWidth + centerOffset + (laneWidth - 40) / 2.0);
        player.setLayoutY(500);
    }

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

            Rectangle obstacle = new Rectangle(40, 40, Color.RED);
            double centerOffset = (WIDTH - laneWidth * lanes) / 2.0;
            obstacle.setLayoutX(lane * laneWidth + centerOffset + (laneWidth - 40) / 2.0);
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

    private void checkCollision() {
        if (lives <= 0) {
            return;  // 如果生命值已經歸零，直接返回
        }
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
        if (gameOver) {
            return;
        }

        // 確保遊戲只在生命值歸零時結束
        if (lives <= 0) {
            endGame();
            return;
        }

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
