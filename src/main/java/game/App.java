package game;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private Stage primaryStage;
    private Scene homeScene, singlePlayerScene, multiPlayerScene, gameOverScene;
    private GameOverPage gameOverPage;
    private SinglePlayerPage singlePlayerPage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("賽車遊戲");

        // 建立各個頁面的實例
        HomePage homePage = new HomePage(this);
        singlePlayerPage = new SinglePlayerPage(this);
        MultiPlayerPage multiPlayerPage = new MultiPlayerPage(this);
        gameOverPage = new GameOverPage(this);

        // 建立場景
        homeScene = new Scene(homePage.createContent(), 600, 800);
        singlePlayerScene = new Scene(singlePlayerPage.createContent(), 600, 800);
        multiPlayerScene = new Scene(multiPlayerPage.createContent(), 600, 800);
        gameOverScene = new Scene(gameOverPage.createContent(), 600, 800);

        // 顯示首頁
        primaryStage.setScene(homeScene);
        primaryStage.show();
    }

    // 切換到單人模式頁面
    public void switchToSinglePlayer() {
        singlePlayerPage = new SinglePlayerPage(this);
        singlePlayerScene = new Scene(singlePlayerPage.createContent());

        // 在場景層級也添加鍵盤事件處理
        singlePlayerScene.setOnKeyPressed(event -> singlePlayerPage.handleKeyPress(event));

        primaryStage.setScene(singlePlayerScene);
    }

    // 切換到連線模式頁面
    public void switchToMultiPlayer() {
        primaryStage.setScene(multiPlayerScene);
    }

    // 切換到遊戲結束頁面
    public void switchToGameOver() {
        gameOverPage.setScores(singlePlayerPage.getScore(), singlePlayerPage.getHighScore());
        // 刷新 GameOverPage 的內容
        gameOverScene = new Scene(gameOverPage.createContent());
        primaryStage.setScene(gameOverScene);
    }

    // 返回首頁 (如果需要)
    public void switchToHomePage() {
        primaryStage.setScene(homeScene);
    }

    // 重新開始遊戲
    public void restartGame() {
        // 創建新的單人遊戲頁面
        singlePlayerPage = new SinglePlayerPage(this);
        singlePlayerScene = new Scene(singlePlayerPage.createContent());

        // 設定鍵盤控制
        singlePlayerScene.setOnKeyPressed(singlePlayerPage::handleKeyPress);
        //singlePlayerScene.setOnKeyReleased(singlePlayerPage::handleKeyRelease);

        // 切換到新的遊戲場景
        primaryStage.setScene(singlePlayerScene);
    }

    public static void main(String[] args) {
        launch(args);
    }

}
