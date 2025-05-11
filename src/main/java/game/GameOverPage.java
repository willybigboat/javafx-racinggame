package game;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class GameOverPage {

    private App app;

    public GameOverPage(App app) {
        this.app = app;
    }

    public Parent createContent() {
        Label gameOverLabel = new Label("遊戲結束！");
        Button homeButton = new Button("返回首頁");
        homeButton.setOnAction(event -> app.switchToHomePage());

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(50));
        layout.getChildren().addAll(gameOverLabel, homeButton);

        return layout;
    }
}