package game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class SinglePlayerPage {

    private App app;
    private ImageView player;
    private ArrayList<ImageView> obstacles = new ArrayList<>();
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

    private static final double TRACK_START_Y = 800 * 0.3;
    private static final double TRACK_BOTTOM_Y = 800;
    private static final double TRACK_TOP_WIDTH = 200;
    private static final double TRACK_BOTTOM_WIDTH = 400;
    private static final int LANES = 4;

    // 在 SinglePlayerPage 類別中新增障礙物圖片清單
    private static final String[] OBSTACLE_IMAGES = {
        "/image/bananaPeel.png",
        "/image/can.png",
        "/image/garbage.png",
        "/image/redBlock.png",
        "/image/yellowBlock.png"
    };

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
            if (timer != null) {
                timer.stop();
            }
            app.switchToHomePage();
        });

        // 創建主要的 StackPane 而不是 Pane，這樣可以更好地處理大小變化
        StackPane root = new StackPane();
        root.setPrefSize(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);

        // 綁定背景畫布的大小
        backgroundCanvas = new RaceTrackCanvas(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
        backgroundCanvas.widthProperty().bind(root.widthProperty());
        backgroundCanvas.heightProperty().bind(root.heightProperty());

        // 綁定遊戲層的大小
        gamePane = new Pane();
        gamePane.prefWidthProperty().bind(root.widthProperty());
        gamePane.prefHeightProperty().bind(root.heightProperty());

        // 監聽寬度變化，自動重新置中
        gamePane.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0) {
                updatePlayerPosition();
            }
        });

        // 玩家位置綁定（用圖片）
        Image carImage = new Image(getClass().getResourceAsStream("/image/redCar.png"));
        player = new ImageView(carImage);
        player.setFitWidth(60);
        player.setFitHeight(90);

        // 分數文字位置綁定
        scoreText = new Text("Score: 0");
        highScoreText = new Text("High Score: " + highScore);
        lifeText = new Text("Lives: 3");

        // 用 VBox 包住三個文字，並設透明白色背景
        VBox infoBox = new VBox(10, scoreText, highScoreText, lifeText);
        infoBox.setAlignment(Pos.TOP_LEFT);
        infoBox.setPadding(new Insets(10));
        infoBox.setStyle("-fx-background-color: rgba(255,255,255,0.7); -fx-background-radius: 10px;");
        infoBox.setLayoutX(30);
        infoBox.setLayoutY(30);

        gameOverText = new Text();
        gameOverText.layoutXProperty().bind(
                root.widthProperty().divide(2).subtract(70)
        );
        gameOverText.layoutYProperty().bind(
                root.heightProperty().multiply(0.4)
        );

        gamePane.getChildren().clear(); // 清空畫面
        gamePane.getChildren().addAll(player, infoBox, gameOverText);

        // 停止舊計時器（如果存在）
        if (timer != null) {
            timer.stop();
        }

        root.getChildren().add(uiBox);
        root.getChildren().addAll(backgroundCanvas, gamePane);

        // 初始化玩家位置
        updatePlayerPosition();

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
        double playerY = TRACK_BOTTOM_Y - player.getFitHeight() - 50; // 固定Y
        double trackWidth = getTrackWidthAtY(playerY);
        double laneWidth = trackWidth / LANES;
        double trackCenterX = getTrackCenterX();
        double trackLeftX = trackCenterX - trackWidth / 2.0;

        double playerX = trackLeftX + currentLane * laneWidth + (laneWidth - player.getFitWidth()) / 2.0;
        player.setLayoutX(playerX);
        player.setLayoutY(playerY);
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

            // 隨機選擇一個障礙物圖片
            String imgPath = OBSTACLE_IMAGES[rand.nextInt(OBSTACLE_IMAGES.length)];
            Image obsImg = new Image(getClass().getResourceAsStream(imgPath));
            ImageView obstacle = new ImageView(obsImg);
            obstacle.setFitWidth(60);
            obstacle.setFitHeight(60);

            double obstacleY = TRACK_START_Y; // 障礙物初始 Y
            double trackWidth = getTrackWidthAtY(obstacleY);
            double laneWidth = trackWidth / LANES;
            double trackCenterX = getTrackCenterX();
            double trackLeftX = trackCenterX - trackWidth / 2.0;

            double obstacleX = trackLeftX + lane * laneWidth + (laneWidth - obstacle.getFitWidth()) / 2.0;
            obstacle.setLayoutX(obstacleX);
            obstacle.setLayoutY(obstacleY); // 讓障礙物從賽道頂端出現
            obstacle.setUserData(lane); // 記錄這個障礙物屬於哪個車道
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
            double newY = obs.getLayoutY() + speed;
            obs.setLayoutY(newY);

            // 重新計算 X，讓障礙物隨賽道寬度變化
            // 你需要知道這個障礙物在第幾車道
            Integer lane = (Integer) obs.getUserData();
            if (lane != null) {
                double trackWidth = getTrackWidthAtY(newY);
                double laneWidth = trackWidth / LANES;
                double trackCenterX = getTrackCenterX();
                double trackLeftX = trackCenterX - trackWidth / 2.0;
                double obstacleX = trackLeftX + lane * laneWidth + (laneWidth - obs.getFitWidth()) / 2.0;
                obs.setLayoutX(obstacleX);
            }

            if (newY > HEIGHT) {
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
        Iterator<ImageView> iter = obstacles.iterator();
        while (iter.hasNext()) {
            ImageView obs = iter.next();
            if (isCollision(player, obs)) {
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

    private boolean isCollision(ImageView player, ImageView obs) {
        // 取得玩家和障礙物的邊界
        double px = player.getLayoutX();
        double py = player.getLayoutY();
        double pw = player.getFitWidth();
        double ph = player.getFitHeight();

        double ox = obs.getLayoutX();
        double oy = obs.getLayoutY();
        double ow = obs.getFitWidth();
        double oh = obs.getFitHeight();

        // 縮小判斷區域，例如只用中間 70%
        double marginP = 0.15, marginO = 0.15;
        px += pw * marginP;
        pw *= (1 - 2 * marginP);
        py += ph * marginP;
        ph *= (1 - 2 * marginP);

        ox += ow * marginO;
        ow *= (1 - 2 * marginO);
        oy += oh * marginO;
        oh *= (1 - 2 * marginO);

        return px < ox + ow && px + pw > ox && py < oy + oh && py + ph > oy;
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

    private double getTrackWidthAtY(double y) {

        // 線性插值
        double t = (y - TRACK_START_Y) / (TRACK_BOTTOM_Y - TRACK_START_Y);
        t = Math.max(0, Math.min(1, t));

        return TRACK_TOP_WIDTH + (TRACK_BOTTOM_WIDTH - TRACK_TOP_WIDTH) * t;
    }

    private double getTrackCenterX() {
        return App.WINDOW_WIDTH / 2.0;
    }
}
