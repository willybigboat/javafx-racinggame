package game;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class SinglePlayerPage {

    private App app;

    public SinglePlayerPage(App app) {
        this.app = app;
    }

    @SuppressWarnings("exports")
    public Parent createContent() {
        Label titleLabel = new Label("單人模式");
        Button backButton = new Button("返回首頁");
        backButton.setOnAction(event -> app.switchToHomePage());

        // 這裡可以加入單人遊戲的內容 (例如：Canvas, 控制按鈕等)
        Label gameContent = new Label("單人遊戲內容將在此顯示...");

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(50));
        layout.getChildren().addAll(titleLabel, gameContent, backButton);

        return layout;
    }

    // 在單人遊戲結束後調用此方法
    public void endGame() {
        app.switchToGameOver();
    }
}