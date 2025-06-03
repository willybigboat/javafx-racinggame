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

        primaryStage.setMinWidth(WINDOW_WIDTH);
        primaryStage.setMinHeight(WINDOW_HEIGHT);

        HomePage homePage = new HomePage(this);
        singlePlayerPage = new SinglePlayerPage(this);
        multiPlayerPage = new MultiPlayerPage(this);
        gameOverPage = new GameOverPage(this);
        mulgameOverPage = new mulGameOverPage(this);

        homeScene = new Scene(homePage.createContent(), WINDOW_WIDTH, WINDOW_HEIGHT);
        singlePlayerScene = new Scene(singlePlayerPage.createContent(), WINDOW_WIDTH, WINDOW_HEIGHT);
        multiPlayerScene = new Scene(multiPlayerPage.createContent(), WINDOW_WIDTH, WINDOW_HEIGHT);
        gameOverScene = new Scene(gameOverPage.createContent(), WINDOW_WIDTH, WINDOW_HEIGHT);
        mulgameOverScene = new Scene(mulgameOverPage.createContent(), WINDOW_WIDTH, WINDOW_HEIGHT);

        primaryStage.setScene(homeScene);
        primaryStage.show();
    }

    public void switchToSinglePlayer() {
        boolean wasFullScreen = primaryStage.isFullScreen();

        singlePlayerScene = new Scene(singlePlayerPage.createContent(), WINDOW_WIDTH, WINDOW_HEIGHT);
        singlePlayerScene.setOnKeyPressed(event -> singlePlayerPage.handleKeyPress(event));
        primaryStage.setScene(singlePlayerScene);

        if (wasFullScreen) {
            primaryStage.setFullScreen(true);
        }

        singlePlayerPage.startGame();
    }

    public void switchToMultiPlayer() {
        resetNetworkManager();
        MultiPlayerPage multiPlayerPage = new MultiPlayerPage(this);
        multiPlayerScene = new Scene(multiPlayerPage.createContent());
        multiPlayerScene.setOnKeyPressed(event -> multiPlayerPage.handleKeyPress(event));
        primaryStage.setScene(multiPlayerScene);
    }

    public void switchToMultiPlayerWaitingPage() {
        if (multiPlayerPage == null) {
            multiPlayerPage = new MultiPlayerPage(this);
        }
        Scene scene = new Scene(multiPlayerPage.createContent(), WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setScene(scene);
        multiPlayerPage.showWaitingPage();
    }

    public void switchToGameOver() {
        gameOverPage.setScores(singlePlayerPage.getScore(), singlePlayerPage.getHighScore());
        gameOverScene = new Scene(gameOverPage.createContent());
        primaryStage.setScene(gameOverScene);
    }

    // 返回首頁
    public void switchToHomePage() {
        resetNetworkManager();
        HomePage homePage = new HomePage(this);
        setScene(new Scene(homePage.createContent()));
    }

    public void restartGame() {
        switchToSinglePlayer();
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