package game;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class GameOverPage {

    private App app;
    private int finalScore;
    private int highScore;

    public GameOverPage(App app) {
        this.app = app;
    }

    public void setScores(int finalScore, int highScore) {
        this.finalScore = finalScore;
        this.highScore = highScore;
    }

    public Parent createContent() {
        Label gameOverLabel = new Label("遊戲結束！");
        gameOverLabel.setFont(Font.font(48));
        
        Label scoreLabel = new Label("最終得分：" + finalScore);
        scoreLabel.setFont(Font.font(24));
        
        Label highScoreLabel = new Label("最高分數：" + highScore);
        highScoreLabel.setFont(Font.font(24));
        
        Button restartButton = new Button("重新開始");
        restartButton.setStyle(
            "-fx-font-size: 20px;" +
            "-fx-background-radius: 20px;" +
            "-fx-background-color: linear-gradient(to bottom, #43e97b, #38f9d7);" +
            "-fx-text-fill: white"
        );
        restartButton.setOnAction(event -> app.restartGame());
        
        Button homeButton = new Button("返回首頁");
        homeButton.setStyle(
            "-fx-font-size: 20px;" +
            "-fx-background-radius: 20px;" +
            "-fx-background-color: linear-gradient(to bottom, #2196F3, #6DD5FA);" +
            "-fx-text-fill: white"
        );
        homeButton.setOnAction(event -> app.switchToHomePage());

        VBox layout = new VBox(30);
        layout.setPrefSize(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(50));
        layout.getChildren().addAll(
            gameOverLabel, 
            scoreLabel, 
            highScoreLabel, 
            restartButton, 
            homeButton
        );

        return layout;
    }
}