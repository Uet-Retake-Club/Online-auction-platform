package com.auction.server;

import com.auction.server.network.ClientHandler;
import com.auction.server.services.AuctionManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerApplication {
    private static final int PORT = 8080;
    // Tối ưu hóa: Dùng CachedThreadPool để tự động quản lý, cấp phát và thu hồi
    // luồng
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        System.out.println("[SERVER] Khởi động Online Auction Server...");

        // Giup tat Pool khi dot nhien tat may
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SERVER] Nhận lệnh tắt máy. Đang dọn dẹp...");
            threadPool.shutdown();
            // goi lenh tat AutoBidThread
            AuctionManager.getInstance().shutdown();

            try {
                if (!threadPool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
            }
            System.out.println("[SERVER] Đã đóng cửa an toàn.");
        }));

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] Đang lắng nghe kết nối tại cổng " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[NETWORK] Khách hàng mới kết nối: " + clientSocket.getInetAddress());

                // Ném nhiệm vụ xử lý Client này vào Thread Pool
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("[SERVER ERROR] Lỗi cổng mạng: " + e.getMessage());
        } finally {
            System.out.println("[SERVER] Luồng chính đã dừng do lỗi mạng. Đang ép buộc hệ thống tắt...");

            // Lệnh này bắt buộc JVM phải tắt ngay lập tức.
            // Và ngay khi JVM chuẩn bị tắt, nó SẼ TỰ ĐỘNG GỌI CÁI SHUTDOWN HOOK mà bạn đã
            // cài ở trên!
            System.exit(1);
        }
    }
}