package game;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import game.UIUtils;

public class HomePage {

    private App app;

    public HomePage(App app) {
        this.app = app;
    }

    @SuppressWarnings("exports")
    public Parent createContent() {
        RaceTrackCanvas canvas = new RaceTrackCanvas(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);        

        Label titleLabel = new Label("賽車遊戲");
        UIUtils.applyTitleLabel(titleLabel);

        Button singlePlayerButton = new Button("單人模式");
        UIUtils.applyMainButton(singlePlayerButton);
        singlePlayerButton.setOnAction(event -> app.switchToSinglePlayer());

        Button multiPlayerButton = new Button("連機模式");
        UIUtils.applySecondaryButton(multiPlayerButton);
        multiPlayerButton.setOnAction(event -> app.switchToMultiPlayer());

        VBox contentBox = new VBox(30, titleLabel, singlePlayerButton, multiPlayerButton);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(50));

        StackPane root = new StackPane(canvas, contentBox);
        // 修正：使用統一的視窗大小常數
        root.setPrefSize(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);

        return root;
    }
}