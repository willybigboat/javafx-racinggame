package game;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

import javafx.scene.shape.Rectangle;

public class NetworkManager {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isHost;
    
    // 遊戲狀態同步用的資料類別
    public static class GameState implements Serializable {
        public int playerLane;
        public int score;
        public int lives;
        public ArrayList<Rectangle> obstacles;
        public boolean gameStarting;  // 新增此欄位
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