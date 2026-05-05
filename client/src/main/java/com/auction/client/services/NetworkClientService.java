package com.auction.client.services;

import com.auction.shared.dto.MessageType;
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
 */
public class NetworkClientService {
    private static NetworkClientService instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson;
    private boolean isRunning = false;

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
            instance = new NetworkClientService();
        }
        return instance;
    }

    public void addListener(ServerMessageListener listener) {
        listeners.add(listener);
    }

    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            isRunning = true;
            
            System.out.println("✅ Đã kết nối đến Server Đấu giá: " + host + ":" + port);
            startListeningThread();
        } catch (Exception e) {
            System.err.println("❌ Lỗi kết nối Server: " + e.getMessage());
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
                    System.err.println("❌ Mất kết nối tới Server.");
                }
            }
        }, "Client-Listen-Thread").start();
    }

    public void sendRequest(Request request) {
        if (out != null && isRunning) {
            String jsonRequest = gson.toJson(request);
            out.println(jsonRequest);
        } else {
            System.err.println(" Không thể gửi, Socket chưa kết nối!");
        }
    }

    public void disconnect() {
        isRunning = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println(" Đã ngắt kết nối an toàn.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}