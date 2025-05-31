package game;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class NetworkManager {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isHost;
    
    // 可序列化的障礙物類別
    public static class SerializableObstacle implements Serializable {
        public double x;
        public double y;
        public double width;
        public double height;
        public String imagePath; // 新增圖片路徑欄位
        
        
        // 由 ImageView 建立
        public SerializableObstacle(ImageView obs) {
            this.imagePath = (String) obs.getUserData(); // 取得圖片路徑
            this.width = obs.getFitWidth();
            this.height = obs.getFitHeight();
            this.x = obs.getLayoutX();
            this.y = obs.getLayoutY();
        }            
        
        public ImageView toImageView() {
            // 需要正確還原圖片路徑與位置
            Image img = new Image(getClass().getResourceAsStream(this.imagePath));
            ImageView view = new ImageView(img);
            view.setFitWidth(this.width);
            view.setFitHeight(this.height);
            view.setLayoutX(this.x);
            view.setLayoutY(this.y);
            view.setUserData(this.imagePath); // 保持一致
            return view;
        }
    }
    
    // 遊戲狀態同步用的資料類別
    public static class GameState implements Serializable {
        public int playerLane;
        public int score;
        public int lives;
        public ArrayList<SerializableObstacle> obstacles;
        public boolean gameStarting;  // 新增此欄位
        public boolean isReady;
    }
    
    // 建立主機
    public void createHost(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        socket = serverSocket.accept();
        initializeStreams();
        isHost = true;
    }
    
    // 加入遊戲
    public void joinGame(String host, int port) throws IOException {
        socket = new Socket(host, port);
        initializeStreams();
        isHost = false;
    }
    
    private void initializeStreams() throws IOException {
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }
    
    // 傳送遊戲狀態
    public void sendGameState(GameState state) throws IOException {
        out.writeObject(state);
        out.flush();
    }
    
    // 接收遊戲狀態
    public GameState receiveGameState() throws IOException, ClassNotFoundException {
        return (GameState) in.readObject();
    }
    
    public void close() throws IOException {
        if (socket != null) {
            socket.close();
        }
        if (out != null) {
            out.close();
        }
        if (in != null) {
            in.close();
        }
    }

}