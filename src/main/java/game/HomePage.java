package game;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class HomePage {

    private App app;

    public HomePage(App app) {
        this.app = app;
    }

    @SuppressWarnings("exports")
    public Parent createContent() {
        RaceTrackCanvas canvas = new RaceTrackCanvas(600, 1000);

        Label titleLabel = new Label("賽車遊戲");
        titleLabel.setStyle("-fx-font-size: 60px; -fx-font-weight: bold; -fx-text-fill: #222; -fx-effect: dropshadow(gaussian, #FFD700, 10, 0.5, 0, 0);");

        Button singlePlayerButton = new Button("單人模式");
        singlePlayerButton.setStyle(
            "-fx-font-size: 40px;" +
            "-fx-background-radius: 50px;" +
            "-fx-background-color: linear-gradient(to bottom, #43e97b, #38f9d7);" +
            "-fx-text-fill: white;" +
            "-fx-effect: dropshadow(gaussian, #222, 8, 0.5, 0, 2);"
        );
        singlePlayerButton.setOnAction(event -> app.switchToSinglePlayer());

        Button multiPlayerButton = new Button("連線模式");
        multiPlayerButton.setStyle(
            "-fx-font-size: 40px;" +
            "-fx-background-radius: 50px;" +
            "-fx-background-color: linear-gradient(to bottom, #2196F3, #6DD5FA);" +
            "-fx-text-fill: white;" +
            "-fx-effect: dropshadow(gaussian, #222, 8, 0.5, 0, 2);"
        );
        multiPlayerButton.setOnAction(event -> app.switchToMultiPlayer());

        VBox contentBox = new VBox(30, titleLabel, singlePlayerButton, multiPlayerButton);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(50));
        contentBox.setMouseTransparent(true);

        StackPane root = new StackPane(canvas, contentBox);
        root.setPrefSize(600, 1000);

        return root;
    }
}