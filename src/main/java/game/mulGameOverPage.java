package game;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class mulGameOverPage {

    private App app;
    private int player1Score;
    private int player2Score;

    public mulGameOverPage(App app) {
        this.app = app;
    }

    public void setScores(int player1Score, int player2Score) {
        this.player1Score = player1Score;
        this.player2Score = player2Score;
    }

    public Parent createContent() {
        // 建立背景 Canvas
        RaceTrackCanvas backgroundCanvas = new RaceTrackCanvas(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);

        // 遊戲結束標題
        Label gameOverLabel = new Label("遊戲結束！");
        gameOverLabel.setFont(Font.font(48));

        Label player1ScoreLabel = new Label("玩家1分數：" + player1Score);
        UIUtils.applyScoreLabel(player1ScoreLabel);

        Label player2ScoreLabel = new Label("玩家2分數：" + player2Score);
        UIUtils.applyScoreLabel(player2ScoreLabel);

        Label scoreLabel = new Label();
        UIUtils.applyScoreLabel(scoreLabel);

        if (player1Score > player2Score) {
            scoreLabel.setText("玩家1獲勝！");
        } else if (player1Score < player2Score) {
            scoreLabel.setText("玩家2獲勝！");
        } else {
            scoreLabel.setText("平局！");
        }

        /*
         * Button restartButton = new Button("重新開始");
         * UIUtils.applyrestartButton(restartButton);
         * restartButton.setOnAction(event -> app.restartGame());
         */

        Button homeButton = new Button("返回首頁");
        UIUtils.applybackButton(homeButton);
        homeButton.setOnAction(event -> {
            // 先關閉網路連線，再跳轉頁面
            try {
                if (app instanceof App) {
                    App appInstance = (App) app;
                    if (appInstance.getCurrentNetworkManager() != null) {
                        appInstance.getCurrentNetworkManager().close();
                        System.out.println("返回主頁時關閉網路連線");
                        // 確保關閉後再跳轉
                        appInstance.setCurrentNetworkManager(null);
                    }
                }
            } catch (Exception e) {
                System.err.println("關閉網路連線時發生錯誤: " + e);
            }

            app.switchToHomePage();
        });

        // 白色透明框框包住內容
        VBox contentBox = new VBox(20, gameOverLabel, player1ScoreLabel, player2ScoreLabel, scoreLabel, homeButton);
        contentBox.setMinWidth(App.WINDOW_WIDTH / 2);
        contentBox.setMaxWidth(App.WINDOW_WIDTH / 2);
        contentBox.setMinHeight(App.WINDOW_HEIGHT / 2.5);
        contentBox.setMaxHeight(App.WINDOW_HEIGHT / 2.5);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(50));
        contentBox.setStyle("-fx-background-color: rgba(255,255,255,0.7); -fx-background-radius: 20px;");

        StackPane root = new StackPane(backgroundCanvas, contentBox);
        root.setPrefSize(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);

        return root;
    }
}