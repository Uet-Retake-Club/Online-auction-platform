package com.auction.server;

import com.auction.server.network.ClientHandler;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerApplication {
    private static final int PORT = 8080;
    // Tối ưu hóa: Dùng CachedThreadPool để tự động quản lý, cấp phát và thu hồi luồng
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        System.out.println("🚀 [SERVER] Khởi động Online Auction Server...");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ [SERVER] Đang lắng nghe kết nối tại cổng " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("🌐 [NETWORK] Khách hàng mới kết nối: " + clientSocket.getInetAddress());

                // Ném nhiệm vụ xử lý Client này vào Thread Pool
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("❌ [SERVER ERROR] Lỗi cổng mạng: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }
}