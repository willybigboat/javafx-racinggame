package game;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.text.Font;

public class UIUtils {
    /**
     * 設定按鈕樣式並加上懸浮放大效果(主頁)
     */
    public static void applyButtonStyle(Button button, String style) {
        button.setStyle(style);
        button.setOnMouseEntered(e -> button.setStyle(style
                + "-fx-scale-x: 1.1;"
                + "-fx-scale-y: 1.1;"
                + "-fx-cursor: hand;"
        ));
        button.setOnMouseExited(e -> button.setStyle(style));
    }

    /**
     * 套用標準主按鈕樣式(主頁:單人模式)
     */
    public static void applyMainButton(Button button) {
        String style = "-fx-font-size: 40px;"
                + "-fx-background-radius: 50px;"
                + "-fx-background-color: linear-gradient(to bottom, #43e97b, #38f9d7);"
                + "-fx-text-fill: white;"
                + "-fx-effect: dropshadow(gaussian, #222, 8, 0.5, 0, 2);";
        applyButtonStyle(button, style);
    }

    /**
     * 套用重新開始按鈕樣式(遊戲結束頁面)
     */
    public static void applyrestartButton(Button button) {
        String style = "-fx-font-size: 20px;"
                + "-fx-background-radius: 50px;"
                + "-fx-background-color: linear-gradient(to bottom, #43e97b, #38f9d7);"
                + "-fx-text-fill: white;"
                + "-fx-effect: dropshadow(gaussian, #222, 8, 0.5, 0, 2);";
        applyButtonStyle(button, style);
    }

    /**
     * 套用標準副按鈕樣式(主頁:連機模式)
     */
    public static void applySecondaryButton(Button button) {
        String style = "-fx-font-size: 40px;"
                + "-fx-background-radius: 50px;"
                + "-fx-background-color: linear-gradient(to bottom, #2196F3, #6DD5FA);"
                + "-fx-text-fill: white;"
                + "-fx-effect: dropshadow(gaussian, #222, 8, 0.5, 0, 2);";
        applyButtonStyle(button, style);
    }

    /**
     * 套用標準副按鈕樣式(主頁:等待頁面)
     */
    public static void applywaitButton(Button button) {
        String style = "-fx-font-size: 20px;"
                + "-fx-background-radius: 50px;"
                + "-fx-background-color: linear-gradient(to bottom, #43e97b, #38f9d7);"
                + "-fx-text-fill: white;"
                + "-fx-effect: dropshadow(gaussian, #222, 8, 0.5, 0, 2);";;
        applyButtonStyle(button, style);
    }
    /**
     * 套用返回首頁按鈕樣式
     */ 
    public static void applybackButton(Button button) {
        String style = "-fx-font-size: 20px;"
                + "-fx-background-radius: 50px;"
                + "-fx-background-color: linear-gradient(to bottom, #2196F3, #6DD5FA);"
                + "-fx-text-fill: white;"
                + "-fx-effect: dropshadow(gaussian, #222, 8, 0.5, 0, 2);";;
        applyButtonStyle(button, style);
    }

    /**
     * 套用取消按鈕樣式（多人）
     */
    public static void applyCancelButton(Button button) {
        String style = "-fx-font-size: 20px;"
                + "-fx-background-radius:50px;"
                + "-fx-background-color: #FF5722;"
                + "-fx-text-fill: white;";
        applyButtonStyle(button, style);
    }

    /**
     * 套用標題樣式(主頁)
     */
    public static void applyTitleLabel(Label label) {
        label.setFont(Font.font(60));
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #222");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
    }

    /**
     * 套用一般分數標籤樣式
     */
    public static void applyScoreLabel(Label label) {
        label.setFont(Font.font(24));
        label.setStyle("-fx-text-fill: #333;");
    }
}