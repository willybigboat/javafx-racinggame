package game;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    // 定義統一的視窗大小常數
    public static final double WINDOW_WIDTH = 600;
    public static final double WINDOW_HEIGHT = 800;

    private Stage primaryStage;
    private Scene homeScene, singlePlayerScene, multiPlayerScene, gameOverScene;
    private GameOverPage gameOverPage;
    private SinglePlayerPage singlePlayerPage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("賽車遊戲");

        // 設定視窗最小大小
        primaryStage.setMinWidth(WINDOW_WIDTH);
        primaryStage.setMinHeight(WINDOW_HEIGHT);

        // 建立各個頁面的實例
        HomePage homePage = new HomePage(this);
        singlePlayerPage = new SinglePlayerPage(this);
        MultiPlayerPage multiPlayerPage = new MultiPlayerPage(this);
        gameOverPage = new GameOverPage(this);

        // 建立場景並使用統一大小
        homeScene = new Scene(homePage.createContent(), WINDOW_WIDTH, WINDOW_HEIGHT);
        singlePlayerScene = new Scene(singlePlayerPage.createContent(), WINDOW_WIDTH, WINDOW_HEIGHT);
        multiPlayerScene = new Scene(multiPlayerPage.createContent(), WINDOW_WIDTH, WINDOW_HEIGHT);
        gameOverScene = new Scene(gameOverPage.createContent(), WINDOW_WIDTH, WINDOW_HEIGHT);

        // 顯示首頁
        primaryStage.setScene(homeScene);
        primaryStage.show();
    }

    // 切換到單人模式頁面
    public void switchToSinglePlayer() {        
        singlePlayerScene = new Scene(singlePlayerPage.createContent());

        // 在場景層級也添加鍵盤事件處理
        singlePlayerScene.setOnKeyPressed(event -> singlePlayerPage.handleKeyPress(event));

        primaryStage.setScene(singlePlayerScene);

        // 在場景切換完成後啟動遊戲
        singlePlayerPage.startGame();
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
        switchToSinglePlayer();  // 使用現有的切換方法
    }

    public static void main(String[] args) {
        launch(args);
    }

}