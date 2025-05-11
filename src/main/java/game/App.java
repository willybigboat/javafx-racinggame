package game;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private Stage primaryStage;
    private Scene homeScene, singlePlayerScene, multiPlayerScene, gameOverScene;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("賽車遊戲");

        // 建立各個頁面的實例
        HomePage homePage = new HomePage(this);
        SinglePlayerPage singlePlayerPage = new SinglePlayerPage(this);
        MultiPlayerPage multiPlayerPage = new MultiPlayerPage(this);
        GameOverPage gameOverPage = new GameOverPage(this);

        // 建立場景
        homeScene = new Scene(homePage.createContent(), 800, 600);
        singlePlayerScene = new Scene(singlePlayerPage.createContent(), 800, 600);
        multiPlayerScene = new Scene(multiPlayerPage.createContent(), 800, 600);
        gameOverScene = new Scene(gameOverPage.createContent(), 800, 600);

        // 顯示首頁
        primaryStage.setScene(homeScene);
        primaryStage.show();
    }

    // 切換到單人模式頁面
    public void switchToSinglePlayer() {
        primaryStage.setScene(singlePlayerScene);
    }

    // 切換到連線模式頁面
    public void switchToMultiPlayer() {
        primaryStage.setScene(multiPlayerScene);
    }

    // 切換到遊戲結束頁面
    public void switchToGameOver() {
        primaryStage.setScene(gameOverScene);
    }

    // 返回首頁 (如果需要)
    public void switchToHomePage() {
        primaryStage.setScene(homeScene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}