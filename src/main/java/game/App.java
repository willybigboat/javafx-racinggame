package game;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    // 定義統一的視窗大小常數
    public static final double WINDOW_WIDTH = 1200;
    public static final double WINDOW_HEIGHT = 800;

    private Stage primaryStage;
    private Scene homeScene, singlePlayerScene, multiPlayerScene, gameOverScene, mulgameOverScene;
    private GameOverPage gameOverPage;
    private SinglePlayerPage singlePlayerPage;
    private MultiPlayerPage multiPlayerPage;
    private mulGameOverPage mulgameOverPage;
    private NetworkManager currentNetworkManager;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // 設定視窗最小大小
        primaryStage.setMinWidth(WINDOW_WIDTH);
        primaryStage.setMinHeight(WINDOW_HEIGHT);

        // 建立各個頁面的實例
        HomePage homePage = new HomePage(this);
        singlePlayerPage = new SinglePlayerPage(this);
        multiPlayerPage = new MultiPlayerPage(this);
        gameOverPage = new GameOverPage(this);
        mulgameOverPage = new mulGameOverPage(this);

        // 建立場景並使用統一大小
        homeScene = new Scene(homePage.createContent(), WINDOW_WIDTH, WINDOW_HEIGHT);
        singlePlayerScene = new Scene(singlePlayerPage.createContent(), WINDOW_WIDTH, WINDOW_HEIGHT);
        multiPlayerScene = new Scene(multiPlayerPage.createContent(), WINDOW_WIDTH, WINDOW_HEIGHT);
        gameOverScene = new Scene(gameOverPage.createContent(), WINDOW_WIDTH, WINDOW_HEIGHT);
        mulgameOverScene = new Scene(mulgameOverPage.createContent(), WINDOW_WIDTH, WINDOW_HEIGHT);

        // 顯示首頁
        primaryStage.setScene(homeScene);
        primaryStage.show();
    }

    // 切換到單人模式頁面
    public void switchToSinglePlayer() {
        // 保存當前是否全螢幕
        boolean wasFullScreen = primaryStage.isFullScreen();

        // 重新建立 SinglePlayerPage 內容
        singlePlayerScene = new Scene(singlePlayerPage.createContent(), WINDOW_WIDTH, WINDOW_HEIGHT);

        // 在場景層級也添加鍵盤事件處理
        singlePlayerScene.setOnKeyPressed(event -> singlePlayerPage.handleKeyPress(event));

        // 切換場景
        primaryStage.setScene(singlePlayerScene);

        // 恢復全螢幕狀態
        if (wasFullScreen) {
            primaryStage.setFullScreen(true);
        }

        // 在場景切換完成後啟動遊戲
        singlePlayerPage.startGame();
    }

    // 切換到連線模式頁面
    public void switchToMultiPlayer() {
        resetNetworkManager();
        MultiPlayerPage multiPlayerPage = new MultiPlayerPage(this);
        multiPlayerScene = new Scene(multiPlayerPage.createContent());
        // 在切換到多人遊戲頁面時添加按鍵事件監聽
        multiPlayerScene.setOnKeyPressed(event -> multiPlayerPage.handleKeyPress(event));

        primaryStage.setScene(multiPlayerScene);

        // 在場景切換完成後啟動遊戲
        // singlePlayerPage.startGame();
    }

    // 直接接到連線畫面(開發用)
    public void switchToMultiPlayerWaitingPage() {
        if (multiPlayerPage == null) {
            multiPlayerPage = new MultiPlayerPage(this);
        }
        Scene scene = new Scene(multiPlayerPage.createContent(), WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setScene(scene);
        // 進入等待頁面
        multiPlayerPage.showWaitingPage();
    }

    // 切換到遊戲結束頁面(單人模式)
    public void switchToGameOver() {
        gameOverPage.setScores(singlePlayerPage.getScore(), singlePlayerPage.getHighScore());
        // 刷新 GameOverPage 的內容
        gameOverScene = new Scene(gameOverPage.createContent());
        primaryStage.setScene(gameOverScene);
    }

    // 切換到遊戲結束頁面(連機模式)
    /*
     * public void switchToMulGameOver() {
     * mulgameOverPage.setScores(multiPlayerPage.getScore(), 0);
     * // 刷新 GameOverPage 的內容
     * mulgameOverScene = new Scene(mulgameOverPage.createContent());
     * primaryStage.setScene(mulgameOverScene);
     * }
     */

    // 返回首頁 (如果需要)
    public void switchToHomePage() {
        resetNetworkManager();
        HomePage homePage = new HomePage(this);
        setScene(new Scene(homePage.createContent()));
    }

    // 重新開始遊戲
    public void restartGame() {
        switchToSinglePlayer(); // 使用現有的切換方法
    }

    public void setScene(Scene scene) {
        if (primaryStage != null) {
            primaryStage.setScene(scene);
        }
    }

    public NetworkManager getCurrentNetworkManager() {
        return currentNetworkManager;
    }

    public void setCurrentNetworkManager(NetworkManager networkManager) {
        this.currentNetworkManager = networkManager;
    }

    public void resetNetworkManager() {
        try {
            if (currentNetworkManager != null) {
                currentNetworkManager.close();
                System.out.println("NetworkManager 已重置");
            }
        } catch (IOException e) {
            System.err.println("重置 NetworkManager 時發生錯誤: " + e.getMessage());
        } finally {
            currentNetworkManager = null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
