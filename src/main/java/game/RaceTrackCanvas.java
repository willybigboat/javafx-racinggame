package game;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

public class RaceTrackCanvas extends Canvas {

    private static final double WIDTH = 1200;
    private static final double HEIGHT = 800;
    private static final int LANES = 4;

    // 修改賽道透視參數
    private static final double TRACK_START_Y = HEIGHT * 0.3; // 調整起始位置更靠上
    private static final double TRACK_BOTTOM_Y = HEIGHT; // 調整底部位置
    private static final double TRACK_BOTTOM_WIDTH = 400;
    private static final double TRACK_TOP_WIDTH = 200;
    private static final double GRASS_EXTRA_WIDTH = 80;

    private double offset = 0;
    private double speed = 2;

    public RaceTrackCanvas(double width, double height) {
        super(width, height);

        // 監聽畫布大小變化
        widthProperty().addListener((obs, oldVal, newVal) -> draw());
        heightProperty().addListener((obs, oldVal, newVal) -> draw());

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
        //offset = offset % getWidth();
        //if (offset > 30)
        //  offset = 0;
    }

    private void render() {
        draw();
    }

    private void draw() {
        // 使用當前實際寬高
        double width = getWidth();
        double height = getHeight();

        // 重新繪製賽道
        GraphicsContext gc = getGraphicsContext2D();

        // 繪製天空漸層
        LinearGradient skyGradient = new LinearGradient(
                0, 0,
                0, TRACK_START_Y, // 調整漸層結束位置
                false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#87CEEB")),
                new Stop(1, Color.web("#E0F6FF")));
        gc.setFill(skyGradient);
        gc.fillRect(0, 0, width, height);

        // 繪製遠處的雲朵
        drawClouds(gc);

        // 繪製草地
        gc.setFill(Color.web("#90EE90"));
        gc.fillRect(0, TRACK_START_Y, width, height - TRACK_START_Y);

        // 繪製賽道主體（梯形）
        double[] xPoints = {
                (width - TRACK_BOTTOM_WIDTH) / 2,
                (width - TRACK_TOP_WIDTH) / 2,
                (width + TRACK_TOP_WIDTH) / 2,
                (width + TRACK_BOTTOM_WIDTH) / 2
        };
        double[] yPoints = {
                TRACK_BOTTOM_Y, // 修改底部位置
                TRACK_START_Y,
                TRACK_START_Y,
                TRACK_BOTTOM_Y // 修改底部位置
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
            gc.strokeLine(startX, TRACK_BOTTOM_Y, endX, TRACK_START_Y);
        }
        gc.setLineDashes(null);

        // 繪製賽道邊線
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(3);
        gc.strokeLine(xPoints[0], TRACK_BOTTOM_Y, xPoints[1], TRACK_START_Y);
        gc.strokeLine(xPoints[3], TRACK_BOTTOM_Y, xPoints[2], TRACK_START_Y);

        // 修改樹和草叢的繪製位置
        drawSideElements(gc, new double[] {
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
        gc.fillOval(x - crownSize / 2 + trunkWidth / 2, y - trunkHeight - crownSize / 2,
                crownSize, crownSize);
    }

    private void drawClouds(GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        // (offset * 0.5) 讓雲的移動速度是 offset 速度的一半，這是可以的。
        // 使用 % getWidth() 來確保雲的 X 座標在畫布內循環。
        // 為了處理 cloudEffectiveX 可能為負數的情況 (當 offset*0.5 較小時，減去一個大的 X 值)，
        // 可以使用 ((value % divisor) + divisor) % divisor 的技巧來確保結果總是在 [0, divisor-1] 範圍內。
        double currentWidth = getWidth(); // 獲取當前畫布寬度
        double cloudEffectiveX = offset * 0.5;
        double width = getWidth();
        double[] baseX = { 0.1, 0.5, 0.9, 1.1, 1.5, 1.9 };
        double[] baseY = { 0.1, 0.05, 0.15, 0.1, 0.05, 0.15 };
        double[] scale = { 1.2, 1, 0.8, 1.2, 1, 0.8 };
        double cloudSpeed = offset * 0.5;
        double cycleWidth = width * (baseX[baseX.length - 1] - baseX[0]); // 一個循環的寬度

        
        for (int i = 0; i < baseX.length; i++) {
        double x = width * baseX[i] - cloudSpeed;
        // 讓雲朵自然滑出左側，並在右側循環
        while (x < -150) x += cycleWidth + 200; // 150/200為雲大致寬度，避免閃現
        while (x > width + 150) x -= cycleWidth + 200;
        drawCloud(gc, x, HEIGHT * baseY[i], scale[i]);
    }
        /*
        
        // 繪製雲朵，使其能夠從右邊移入，左邊移出，並循環
        // 範例：三朵雲作為一個重複的模式
        // 雲朵的基礎 X 位置 + 偏移量，然後對寬度取模
        drawCloud(gc, (currentWidth * 0.1 - cloudEffectiveX % currentWidth + currentWidth) % currentWidth, HEIGHT * 0.1,
                1.2);
        drawCloud(gc, (currentWidth * 0.5 - cloudEffectiveX % currentWidth + currentWidth) % currentWidth,
                HEIGHT * 0.05, 1);
        drawCloud(gc, (currentWidth * 0.9 - cloudEffectiveX % currentWidth + currentWidth) % currentWidth,
                HEIGHT * 0.15, 0.8); // 這朵雲可能一開始部分在畫面外

        // 你也可以增加更多雲朵，或者調整它們的初始相對位置和大小
        // 例如，模擬原有多組雲的模式：
        drawCloud(gc, (currentWidth * 1.1 - cloudEffectiveX % currentWidth + currentWidth) % currentWidth, HEIGHT * 0.1,
                1.2); // 相當於接續第一朵雲
        drawCloud(gc, (currentWidth * 1.5 - cloudEffectiveX % currentWidth + currentWidth) % currentWidth,
                HEIGHT * 0.05, 1);
        drawCloud(gc, (currentWidth * 1.9 - cloudEffectiveX % currentWidth + currentWidth) % currentWidth,
                HEIGHT * 0.15, 0.8);
                */
    }

    private void drawCloud(GraphicsContext gc, double x, double y, double scale) {
        double baseSize = 30 * scale;

        // 繪製雲朵的圓形組合
        gc.fillOval(x, y, baseSize, baseSize);
        gc.fillOval(x + baseSize * 0.4, y - baseSize * 0.2, baseSize * 1.1, baseSize);
        gc.fillOval(x + baseSize * 0.8, y, baseSize * 0.9, baseSize * 0.9);
        gc.fillOval(x + baseSize * 0.3, y + baseSize * 0.2, baseSize, baseSize * 0.8);
    }
}