package game;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class RaceTrackCanvas extends Canvas {
    
    private static final double WIDTH = 600;  // 加寬畫布
    private static final double HEIGHT = 1000; // 加高畫布
    private static final int LANES = 4;
    private static final double LANE_WIDTH = 120;  // 加寬賽道
    private static final double TRACK_WIDTH = LANE_WIDTH * LANES;
    private static final double TRACK_START_X = (WIDTH - TRACK_WIDTH) / 2;  

    private double offset = 0;
    private double speed = 2;

    public RaceTrackCanvas(double width, double height) {
        super(width, height);
        startAnimation();
    }

    private void startAnimation() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
                render();
            }
        };
        timer.start();
    }

    private void update() {
        offset += speed;
        if (offset > 40) offset = 0;
    }

    private void render() {
        GraphicsContext gc = getGraphicsContext2D();
        
        // 清除畫布並畫漸層背景
        gc.setFill(Color.SKYBLUE);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // 畫遠景山脈
        gc.setFill(Color.web("#87CEEB").darker());
        for(int i = 0; i < 3; i++) {
            double mountainHeight = HEIGHT * 0.3;
            double y = HEIGHT * 0.4 + i * 20;
            gc.beginPath();
            gc.moveTo(0, y + mountainHeight);
            gc.lineTo(WIDTH * 0.2, y + mountainHeight * 0.7);
            gc.lineTo(WIDTH * 0.5, y + mountainHeight);
            gc.lineTo(WIDTH * 0.7, y + mountainHeight * 0.6);
            gc.lineTo(WIDTH, y + mountainHeight);
            gc.lineTo(WIDTH, HEIGHT);
            gc.lineTo(0, HEIGHT);
            gc.closePath();
            gc.fill();
        }

        // 畫草地底色
        gc.setFill(Color.LIMEGREEN);
        gc.fillRect(0, 0, TRACK_START_X, HEIGHT);
        gc.fillRect(TRACK_START_X + TRACK_WIDTH, 0, TRACK_START_X, HEIGHT);

        // 畫更多的樹
        for(int side = 0; side < 2; side++) {
            double startX = side == 0 ? 0 : TRACK_START_X + TRACK_WIDTH;
            double endX = side == 0 ? TRACK_START_X : WIDTH;
            drawTrees(gc, startX, endX);
            drawBushes(gc, startX, endX);
            drawFlowers(gc, startX, endX);
        }

        // 畫賽道（加入路面紋理）
        gc.setFill(Color.DIMGRAY);
        gc.fillRect(TRACK_START_X, 0, TRACK_WIDTH, HEIGHT);
        
        // 加入路面紋理
        gc.setFill(Color.rgb(50, 50, 50));
        for(int i = 0; i < HEIGHT; i += 20) {
            gc.fillRect(TRACK_START_X, i - (offset % 20), TRACK_WIDTH, 10);
        }

        // 畫分隔線
        gc.setStroke(Color.WHITE);
        gc.setLineDashes(20);  // 加長虛線
        for (int i = 1; i < LANES; i++) {
            double x = TRACK_START_X + i * LANE_WIDTH;
            gc.strokeLine(x, 0, x, HEIGHT);
        }
        gc.setLineDashes(null);

        // 畫賽道邊界
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(5);  // 加粗邊線
        gc.strokeLine(TRACK_START_X, 0, TRACK_START_X, HEIGHT);
        gc.strokeLine(TRACK_START_X + TRACK_WIDTH, 0, TRACK_START_X + TRACK_WIDTH, HEIGHT);
    }

    // 修改畫樹的方法，讓樹的大小更合適
    private void drawTrees(GraphicsContext gc, double startX, double endX) {
        for (int i = 0; i < 5; i++) {  // 增加樹的數量
            double x = startX + (i * ((endX - startX) / 5));
            double y = HEIGHT - 100 - (offset % 60);
            
            gc.setFill(Color.SADDLEBROWN);
            gc.fillRect(x + 15, y - 40, 15, 40);
            
            gc.setFill(Color.FORESTGREEN);
            gc.fillOval(x, y - 80, 45, 60);
        }
    }

    // 畫草叢
    private void drawBushes(GraphicsContext gc, double startX, double endX) {
        for (int i = 0; i < 5; i++) {
            double x = startX + (i * ((endX - startX) / 5));
            double y = HEIGHT - 20 - (offset % 30);
            
            gc.setFill(Color.DARKGREEN);
            gc.fillOval(x, y, 20, 15);
            gc.fillOval(x + 10, y - 5, 20, 15);
        }
    }

    // 畫花
    private void drawFlowers(GraphicsContext gc, double startX, double endX) {
        for (int i = 0; i < 8; i++) {
            double x = startX + (i * ((endX - startX) / 8));
            double y = HEIGHT - 10 - (offset % 20);
            
            // 花瓣
            gc.setFill(Color.YELLOW);
            gc.fillOval(x - 2, y - 2, 4, 4);
            gc.setFill(Color.WHITE);
            for (int j = 0; j < 5; j++) {
                double angle = j * (2 * Math.PI / 5);
                gc.fillOval(
                    x + Math.cos(angle) * 4 - 2,
                    y + Math.sin(angle) * 4 - 2,
                    4, 4
                );
            }
        }
    }
}
