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
        RaceTrackCanvas canvas = new RaceTrackCanvas(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);        

        Label titleLabel = new Label("賽車遊戲");
        UIUtils.applyTitleLabel(titleLabel);

        VBox titleBox = new VBox(titleLabel);
        titleBox.setAlignment(Pos.TOP_CENTER);
        titleBox.setPadding(new Insets(100, 0, 0, 0));

        Button singlePlayerButton = new Button("單人模式");
        UIUtils.applyMainButton(singlePlayerButton);
        singlePlayerButton.setOnAction(event -> app.switchToSinglePlayer());

        Button multiPlayerButton = new Button("連機模式");
        UIUtils.applySecondaryButton(multiPlayerButton);
        multiPlayerButton.setOnAction(event -> app.switchToMultiPlayer());

        Button waitpageButton = new Button("等待頁面");
        UIUtils.applySecondaryButton(waitpageButton);
        waitpageButton.setOnAction(event -> app.switchToMultiPlayerWaitingPage());

        VBox buttonBox = new VBox(30, singlePlayerButton, multiPlayerButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(100, 0, 0, 0));

        StackPane root = new StackPane(canvas);
        
        VBox mainBox = new VBox();
        mainBox.setAlignment(Pos.TOP_CENTER);
        mainBox.setPrefSize(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
        mainBox.getChildren().addAll(titleBox, buttonBox);

        root.getChildren().add(mainBox);
        root.setPrefSize(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);

        return root;
    }
}