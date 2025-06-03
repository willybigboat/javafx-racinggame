package game;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class NetworkManager {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isHost;

    private static final Map<String, Image> IMAGE_CACHE = new HashMap<>();

    public static class SerializableObstacle implements Serializable {
        private static final long serialVersionUID = 1L;
        public double x;
        public double y;
        public double width;
        public double height;
        public String imagePath;
        public int lane;

        public SerializableObstacle(ImageView obsView, String imgPath, int l) {
            this.x = obsView.getLayoutX();
            this.y = obsView.getLayoutY();
            this.width = obsView.getFitWidth();
            this.height = obsView.getFitHeight();
            this.imagePath = imgPath;
            this.lane = l;
        }

        public ImageView toImageView() {
            Image img = null;
            if (IMAGE_CACHE.containsKey(this.imagePath)) {
                img = IMAGE_CACHE.get(this.imagePath);
            } else {
                if (this.imagePath != null && !this.imagePath.isEmpty()) {
                    try {
                        img = new Image(getClass().getResourceAsStream(this.imagePath));
                        if (img != null && img.isError()) {
                            img = null;
                        }
                    } catch (Exception e) {
                        img = null;
                    }
                }
                if (img == null) {
                    String defaultImagePath = "/image/redBlock.png";
                    try {
                        img = new Image(getClass().getResourceAsStream(defaultImagePath));
                        if (img != null && img.isError()) {
                            img = null;
                        }
                    } catch (Exception e) {
                        img = null;
                    }
                }
                if (img != null) {
                    IMAGE_CACHE.put(this.imagePath, img);
                }
            }

            ImageView view;
            if (img != null) {
                view = new ImageView(img);
            } else {
                view = new ImageView();
            }

            view.setFitWidth(this.width);
            view.setFitHeight(this.height);
            view.setLayoutX(this.x);
            view.setLayoutY(this.y);
            view.setUserData(this.lane);
            return view;
        }
    }

    public static class GameState implements Serializable {
        private static final long serialVersionUID = 1L;
        public int playerLane;
        public int score;
        public int lives;
        public ArrayList<SerializableObstacle> obstacles;
        public boolean gameStarting;
        public boolean isReady;
        public boolean gameEnded;
    }

    public void createHost(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("主機：等待客戶端連線...");
        socket = serverSocket.accept();
        System.out.println("主機：客戶端已連線！");
        this.isHost = true;
        initializeStreams();
    }

    public void joinGame(String host, int port) throws IOException {
        System.out.println("客戶端：嘗試連線到主機 " + host + ":" + port);
        socket = new Socket(host, port);
        System.out.println("客戶端：已連線到主機！");
        this.isHost = false;
        initializeStreams();
    }

    private void initializeStreams() throws IOException {
        if (isHost) {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
        } else {
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
        }
        System.out.println("網路流已初始化。 isHost: " + isHost);
    }

    public void sendGameState(GameState state) throws IOException {
        out.reset();
        out.writeObject(state);
        out.flush();
    }

    public GameState receiveGameState() throws IOException, ClassNotFoundException {
        GameState state = (GameState) in.readObject();
        return state;
    }

    public void close() throws IOException {
        System.out.println("關閉網路連線...");
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
        out.writeInt(compressedData.length);
        out.write(compressedData);
        out.flush();
    }

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