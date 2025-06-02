package game;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap; // 新增：用於圖片快取
import java.util.Map; // 新增：用於圖片快取

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class NetworkManager {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isHost; // 確保在 createHost 或 joinGame 中正確設定

    // 新增：靜態圖片快取，用於儲存已載入的圖片
    private static final Map<String, Image> IMAGE_CACHE = new HashMap<>();

    // 可序列化的障礙物類別
    public static class SerializableObstacle implements Serializable {
        private static final long serialVersionUID = 1L; // 添加 serialVersionUID
        public double x;
        public double y;
        public double width;
        public double height;
        public String imagePath; // 保留圖片路徑
        public int lane; // 新增車道欄位

        // 修改構造函數以接收 ImageView 和其對應的 imgPath 及 lane
        public SerializableObstacle(ImageView obsView, String imgPath, int l) {
            this.x = obsView.getLayoutX();
            this.y = obsView.getLayoutY();
            this.width = obsView.getFitWidth();
            this.height = obsView.getFitHeight();
            this.imagePath = imgPath; // 直接傳入 imagePath
            this.lane = l; // 直接傳入 lane
        }

        public ImageView toImageView() {
            Image img = null;

            // 1. 嘗試從快取中獲取圖片
            if (IMAGE_CACHE.containsKey(this.imagePath)) {
                img = IMAGE_CACHE.get(this.imagePath);
            } else {
                // 2. 如果快取中沒有，則嘗試載入圖片並放入快取
                if (this.imagePath != null && !this.imagePath.isEmpty()) {
                    try {
                        img = new Image(getClass().getResourceAsStream(this.imagePath));
                        if (img != null && img.isError()) { // 檢查是否載入成功
                            System.err.println("警告：載入圖片發生錯誤: " + this.imagePath);
                            img = null; // 標記為無效圖片
                        }
                    } catch (Exception e) {
                        System.err.println("錯誤：無法載入障礙物圖片: " + this.imagePath + " - " + e.getMessage());
                        img = null; // 標記為無效圖片
                    }
                }

                // 3. 如果圖片載入失敗或為空，嘗試載入預設圖片
                if (img == null) {
                    System.err.println("警告：障礙物圖片 " + this.imagePath + " 載入失敗或為空。將嘗試使用預設圖片。");
                    String defaultImagePath = "/image/redBlock.png"; // 假設這是你的預設障礙物圖片之一
                    try {
                        img = new Image(getClass().getResourceAsStream(defaultImagePath));
                        if (img != null && img.isError()) {
                            System.err.println("警告：載入預設圖片發生錯誤: " + defaultImagePath);
                            img = null; // 標記為無效圖片
                        }
                    } catch (Exception e) {
                        System.err.println("致命錯誤：無法載入預設障礙物圖片: " + defaultImagePath + " - " + e.getMessage());
                        img = null; // 標記為無效圖片
                    }
                }

                // 4. 如果最終成功載入圖片，則將其放入快取
                if (img != null) {
                    IMAGE_CACHE.put(this.imagePath, img);
                } else {
                    System.err.println("嚴重錯誤：所有圖片載入嘗試均失敗，無法為 " + this.imagePath + " 創建圖片。");
                }
            }

            ImageView view;
            if (img != null) {
                view = new ImageView(img);
            } else {
                // 如果 img 仍然為 null，創建一個空的 ImageView，這可能會導致畫面不顯示但不會崩潰
                System.err.println("嚴重錯誤：無法為障礙物創建有效的 ImageView，將使用一個空的 ImageView。");
                view = new ImageView();
            }

            view.setFitWidth(this.width);
            view.setFitHeight(this.height);
            view.setLayoutX(this.x);
            view.setLayoutY(this.y);
            view.setUserData(this.lane); // 將車道資訊存儲到 ImageView 的 userData 中，供客戶端 moveObstacles 使用
            return view;
        }
    }

    // 遊戲狀態同步用的資料類別
    public static class GameState implements Serializable {
        private static final long serialVersionUID = 1L; // 添加 serialVersionUID
        public int playerLane;
        public int score;
        public int lives;
        public ArrayList<SerializableObstacle> obstacles;
        public boolean gameStarting; // 新增此欄位
        public boolean isReady;
        public boolean gameEnded; // 請確保這個欄位也存在於你的 GameState 中
    }

    // 建立主機
    public void createHost(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("主機：等待客戶端連線...");
        socket = serverSocket.accept();
        System.out.println("主機：客戶端已連線！");
        this.isHost = true; // 在初始化串流前設定角色
        initializeStreams();
    }

    // 加入遊戲
    public void joinGame(String host, int port) throws IOException {
        System.out.println("客戶端：嘗試連線到主機 " + host + ":" + port);
        socket = new Socket(host, port);
        System.out.println("客戶端：已連線到主機！");
        this.isHost = false; // 在初始化串流前設定角色
        initializeStreams();
    }

    private void initializeStreams() throws IOException {
        // 根據主機或客戶端身份調整 ObjectInputStream/ObjectOutputStream 的初始化順序
        if (isHost) {
            // 主機：先輸出，再輸入
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // 確保標頭已寫入
            in = new ObjectInputStream(socket.getInputStream());
        } else {
            // 客戶端：先輸入，再輸出
            // 客戶端通常會先等待主機發送的第一個物件（例如，開始訊號或準備訊號）
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // 確保標頭已寫入
        }
        System.out.println("網路流已初始化。 isHost: " + isHost);
    }

    // 傳送遊戲狀態
    public void sendGameState(GameState state) throws IOException {
        // 在每次發送前重置串流，確保發送的是最新的物件狀態
        // 這是解決 StreamCorruptedException 的關鍵之一
        out.reset();
        out.writeObject(state);
        out.flush();
    }

    // 接收遊戲狀態
    public GameState receiveGameState() throws IOException, ClassNotFoundException {
        GameState state = (GameState) in.readObject();
        return state;
    }

    public void close() throws IOException {
        System.out.println("關閉網路連線...");

        // 順序很重要：先關閉串流，再關閉 socket
        try {
            if (out != null) {
                out.close();
                out = null;
                System.out.println("ObjectOutputStream 已關閉。");
            }
        } catch (IOException e) {
            System.err.println("關閉 ObjectOutputStream 時發生錯誤: " + e.getMessage());
        }

        try {
            if (in != null) {
                in.close();
                in = null;
                System.out.println("ObjectInputStream 已關閉。");
            }
        } catch (IOException e) {
            System.err.println("關閉 ObjectInputStream 時發生錯誤: " + e.getMessage());
        }

        try {
            if (socket != null) {
                socket.close();
                socket = null;
                System.out.println("Socket 已關閉。");
            }
        } catch (IOException e) {
            System.err.println("關閉 Socket 時發生錯誤: " + e.getMessage());
        }
    }

    public void sendCompressedGameState(GameState state) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (
            GZIPOutputStream gzipOut = new GZIPOutputStream(byteStream);
            ObjectOutputStream objOut = new ObjectOutputStream(gzipOut)
        ) {
            objOut.writeObject(state);
        }
        byte[] compressedData = byteStream.toByteArray();
        // 先寫入長度，再寫入資料
        out.writeInt(compressedData.length);
        out.write(compressedData);
        out.flush();
    }

    // 接收壓縮後的遊戲狀態
    public GameState receiveCompressedGameState() throws IOException, ClassNotFoundException {
        int length = in.readInt();
        byte[] compressedData = new byte[length];
        in.readFully(compressedData);
        try (
            ByteArrayInputStream byteStream = new ByteArrayInputStream(compressedData);
            GZIPInputStream gzipIn = new GZIPInputStream(byteStream);
            ObjectInputStream objIn = new ObjectInputStream(gzipIn)
        ) {
            return (GameState) objIn.readObject();
        }
    }
}