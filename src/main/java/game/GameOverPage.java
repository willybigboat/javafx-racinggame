package game;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import game.UIUtils;

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
        UIUtils.applyScoreLabel(scoreLabel);
        
        Label highScoreLabel = new Label("最高分數：" + highScore);
        UIUtils.applyScoreLabel(highScoreLabel);
        
        Button restartButton = new Button("重新開始");
        UIUtils.applyMainButton(restartButton);
        //restartButton.setStyle(restartButton.getStyle() + "-fx-font-size: 20px; -fx-background-radius: 20px;");
        restartButton.setOnAction(event -> app.restartGame());
        
        Button homeButton = new Button("返回首頁");
        UIUtils.applySecondaryButton(homeButton);
        //homeButton.setStyle(homeButton.getStyle() + "-fx-font-size: 20px; -fx-background-radius: 20px;");
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