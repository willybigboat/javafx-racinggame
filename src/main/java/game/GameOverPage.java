package game;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
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
        // 建立背景 Canvas
        RaceTrackCanvas backgroundCanvas = new RaceTrackCanvas(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);

        // 遊戲結束標題
        Label gameOverLabel = new Label("遊戲結束！");
        gameOverLabel.setFont(Font.font(48));
        
        Label scoreLabel = new Label("最終得分：" + finalScore);
        UIUtils.applyScoreLabel(scoreLabel);
        
        Label highScoreLabel = new Label("最高分數：" + highScore);
        UIUtils.applyScoreLabel(highScoreLabel);
        
        Button restartButton = new Button("重新開始");
        UIUtils.applyrestartButton(restartButton);
        restartButton.setOnAction(event -> app.restartGame());
        
        Button homeButton = new Button("返回首頁");
        UIUtils.applybackButton(homeButton);
        homeButton.setOnAction(event -> app.switchToHomePage());

        // 白色透明框框包住內容
        VBox contentBox = new VBox(20, gameOverLabel, scoreLabel, highScoreLabel, restartButton, homeButton);
        contentBox.setMinWidth(App.WINDOW_WIDTH/2);
        contentBox.setMaxWidth(App.WINDOW_WIDTH/2);
        contentBox.setMinHeight(App.WINDOW_HEIGHT/2.5);
        contentBox.setMaxHeight(App.WINDOW_HEIGHT/2.5);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(50));
        contentBox.setStyle("-fx-background-color: rgba(255,255,255,0.7); -fx-background-radius: 20px;");

        StackPane root = new StackPane(backgroundCanvas, contentBox);
        root.setPrefSize(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);

        return root;
    }
}