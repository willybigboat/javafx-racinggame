package game;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;

public class RaceTrackCanvas extends Canvas {
    
    private static final double WIDTH = 600;
    private static final double HEIGHT = 800;
    private static final int LANES = 4;
    
    // 賽道透視參數
    private static final double TRACK_BOTTOM_WIDTH = 400;  // 縮小賽道寬度
    private static final double TRACK_TOP_WIDTH = 100;     // 縮小賽道寬度
    private static final double GRASS_EXTRA_WIDTH = 60;    // 草地額外寬度
    private static final double TRACK_START_Y = HEIGHT * 0.2; // 賽道起始位置（頂部）
    private static final double VANISHING_POINT_X = WIDTH / 2; // 消失點X座標

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
        if (offset > 30) offset = 0;
    }

    private void render() {
        GraphicsContext gc = getGraphicsContext2D();
        
        // 清除畫布
        gc.setFill(Color.SKYBLUE);
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        
        // 繪製賽道主體（梯形）
        double[] xPoints = {
            (WIDTH - TRACK_BOTTOM_WIDTH) / 2,
            (WIDTH - TRACK_TOP_WIDTH) / 2,
            (WIDTH + TRACK_TOP_WIDTH) / 2,
            (WIDTH + TRACK_BOTTOM_WIDTH) / 2
        };
        double[] yPoints = {
            HEIGHT,
            TRACK_START_Y,
            TRACK_START_Y,
            HEIGHT
        };
        gc.setFill(Color.DIMGRAY);
        gc.fillPolygon(xPoints, yPoints, 4);

        // 繪製賽道分隔線
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.setLineDashes(20);
        for (int i = 1; i < LANES; i++) {
            double startX = xPoints[0] + (TRACK_BOTTOM_WIDTH * i / LANES);
            double endX = xPoints[1] + (TRACK_TOP_WIDTH * i / LANES);
            gc.strokeLine(startX, HEIGHT, endX, TRACK_START_Y);
        }
        gc.setLineDashes(null);        

        // 繪製賽道邊線
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(3);
        gc.strokeLine(xPoints[0], HEIGHT, xPoints[1], TRACK_START_Y);
        gc.strokeLine(xPoints[3], HEIGHT, xPoints[2], TRACK_START_Y);

        // 繪製內側草地（加寬版本）
        gc.setFill(Color.LIMEGREEN);
        // 左側草地
        gc.fillPolygon(
            new double[]{
                xPoints[0] - GRASS_EXTRA_WIDTH,           // 底部外側
                xPoints[1] - (GRASS_EXTRA_WIDTH * 0.3),   // 頂部外側（因透視效果縮小）
                xPoints[0],                               // 底部內側
                xPoints[1]                                // 頂部內側
            },
            new double[]{
                HEIGHT,
                TRACK_START_Y,
                HEIGHT,
                TRACK_START_Y
            },
            4
        );
        // 右側草地
        gc.fillPolygon(
            new double[]{
                xPoints[3],                               // 底部內側
                xPoints[2],                               // 頂部內側
                xPoints[3] + GRASS_EXTRA_WIDTH,           // 底部外側
                xPoints[2] + (GRASS_EXTRA_WIDTH * 0.3)    // 頂部外側（因透視效果縮小）
            },
            new double[]{
                HEIGHT,
                TRACK_START_Y,
                HEIGHT,
                TRACK_START_Y
            },
            4
        );

        // 修改樹和草叢的繪製位置
        drawSideElements(gc, new double[]{
            xPoints[0] - GRASS_EXTRA_WIDTH,
            xPoints[1] - (GRASS_EXTRA_WIDTH * 0.3),
            xPoints[2] + (GRASS_EXTRA_WIDTH * 0.3),
            xPoints[3] + GRASS_EXTRA_WIDTH
        }, yPoints);
    }

    private void drawSideElements(GraphicsContext gc, double[] trackX, double[] trackY) {
        // 左側元素
        for (int i = 0; i < 8; i++) {
            double perspectiveFactor = (double) i / 8;
            double x = perspectiveFactor * trackX[1] + (1 - perspectiveFactor) * trackX[0];
            double y = perspectiveFactor * trackY[1] + (1 - perspectiveFactor) * trackY[0];
            
            drawTree(gc, x - 30, y, 1 - perspectiveFactor * 0.7);
        }
        
        // 右側元素
        for (int i = 0; i < 8; i++) {
            double perspectiveFactor = (double) i / 8;
            double x = perspectiveFactor * trackX[2] + (1 - perspectiveFactor) * trackX[3];
            double y = perspectiveFactor * trackY[2] + (1 - perspectiveFactor) * trackY[3];
            
            drawTree(gc, x + 10, y, 1 - perspectiveFactor * 0.7);
        }
    }

    private void drawTree(GraphicsContext gc, double x, double y, double scale) {
        double trunkWidth = 10 * scale;
        double trunkHeight = 30 * scale;
        double crownSize = 40 * scale;
        
        gc.setFill(Color.SADDLEBROWN);
        gc.fillRect(x, y - trunkHeight, trunkWidth, trunkHeight);
        
        gc.setFill(Color.FORESTGREEN);
        gc.fillOval(x - crownSize/2 + trunkWidth/2, y - trunkHeight - crownSize/2, 
                   crownSize, crownSize);
    }
}
