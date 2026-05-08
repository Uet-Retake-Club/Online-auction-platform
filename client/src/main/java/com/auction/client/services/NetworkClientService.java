package com.auction.client.services;

import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý kết nối Socket của Client.
 * Áp dụng Singleton Pattern và Observer Pattern cho Realtime Update.
 * 
 * CẢI TIẾN: Thêm logic tự động kết nối lại (reconnect) nếu Server chưa sẵn sàng.
 */
public class NetworkClientService {
    private static NetworkClientService instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson;
    private volatile boolean isRunning = false;
    private volatile boolean isConnecting = false;

    // Lưu host/port để reconnect
    private String serverHost;
    private int serverPort;

    // Danh sách các lớp đang "lắng nghe" dữ liệu từ Server (Observer Pattern)
    private final List<ServerMessageListener> listeners = new ArrayList<>();

    // Interface cho Observer Pattern
    public interface ServerMessageListener {
        void onMessageReceived(Response response);
    }

    private NetworkClientService() {
        this.gson = new Gson();
    }

    public static NetworkClientService getInstance() {
        if (instance == null) {
            synchronized(NetworkClientService.class){
                if (instance == null) {
                    instance = new NetworkClientService();
                }
            }  
        }
        return instance;
    }

    public void addListener(ServerMessageListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public boolean isConnected() {
        return isRunning && socket != null && !socket.isClosed();
    }

    /**
     * Kết nối đến Server. Nếu thất bại, sẽ tự động thử lại trong background.
     */
    public void connect(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
        attemptConnection();
    }

    private void attemptConnection() {
        if (isConnecting) return;
        isConnecting = true;

        new Thread(() -> {
            int retryCount = 0;
            int maxRetries = 10;
            while (retryCount < maxRetries && !isRunning) {
                try {
                    socket = new Socket(serverHost, serverPort);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    isRunning = true;
                    isConnecting = false;

                    System.out.println("[CLIENT] Da ket noi den Server: " + serverHost + ":" + serverPort);
                    startListeningThread();
                    return; // Kết nối thành công, thoát vòng lặp
                } catch (Exception e) {
                    retryCount++;
                    System.err.println("[CLIENT] Ket noi that bai (lan " + retryCount + "/" + maxRetries + "): " + e.getMessage());
                    if (retryCount < maxRetries) {
                        try {
                            Thread.sleep(2000); // Chờ 2 giây rồi thử lại
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            isConnecting = false;
            if (!isRunning) {
                System.err.println("[CLIENT] Khong the ket noi Server sau " + maxRetries + " lan thu. Hay khoi dong Server truoc.");
            }
        }, "Client-Connect-Thread").start();
    }

    /**
     * Thử kết nối lại nếu chưa kết nối (gọi khi Login).
     */
    public void ensureConnected() {
        if (!isConnected() && !isConnecting) {
            attemptConnection();
        }
    }

    // Luồng ngầm lắng nghe Server (Thread-safe notify)
    private void startListeningThread() {
        new Thread(() -> {
            try {
                String line;
                while (isRunning && (line = in.readLine()) != null) {
                    Response response = gson.fromJson(line, Response.class);
                    // Thông báo cho tất cả các Listener (Observer Pattern)
                    for (ServerMessageListener listener : listeners) {
                        listener.onMessageReceived(response);
                    }
                }
            } catch (Exception e) {
                if (isRunning) {
                    System.err.println("[CLIENT] Mat ket noi toi Server.");
                    isRunning = false;
                }
            }
        }, "Client-Listen-Thread").start();
    }

    public void sendRequest(Request request) {
        if (out != null && isRunning) {
            String jsonRequest = gson.toJson(request);
            out.println(jsonRequest);
        } else {
            System.err.println("[CLIENT] Khong the gui, Socket chua ket noi!");
        }
    }

    public void disconnect() {
        isRunning = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("[CLIENT] Da ngat ket noi an toan.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}