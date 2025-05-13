package game;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MultiPlayerPage {

    private App app;
    private StackPane rootPane;

    public MultiPlayerPage (App app) {
        this.app = app;
    }

    @SuppressWarnings("exports")
    public Parent createContent() {
        rootPane = new StackPane();
        rootPane.setPrefSize(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
        showWaitingPage();
        return rootPane;
    }

    // 顯示等待配對頁面
    private void showWaitingPage() {
        Label waitingLabel = new Label("正在等待其他玩家配對...");
        waitingLabel.setStyle("-fx-font-size: 32px; -fx-text-fill: #555;");
        Button cancelButton = new Button("取消");
        cancelButton.setStyle("-fx-font-size: 20px; -fx-background-radius:50px;-fx-background-color: #FF5722; -fx-text-fill: white;");
        cancelButton.setOnAction(event -> app.switchToHomePage());

        VBox waitingLayout = new VBox(30, waitingLabel, cancelButton);
        waitingLayout.setAlignment(Pos.CENTER);
        waitingLayout.setPadding(new Insets(50));

        rootPane.getChildren().setAll(waitingLayout);
    }

    // 配對完成後呼叫此方法顯示遊戲內容
    public void showGameContent() {
        Label titleLabel = new Label("連線模式");
        Button backButton = new Button("返回首頁");
        backButton.setOnAction(event -> app.switchToHomePage());

        Label gameContent = new Label("連線遊戲內容將在此顯示...");

        VBox layout = new VBox(20, titleLabel, gameContent, backButton);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(50));

        rootPane.getChildren().setAll(layout);
    }

    // 在連線遊戲結束後調用此方法
    public void endGame() {
        app.switchToGameOver();
    }
}