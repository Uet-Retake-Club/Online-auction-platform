package com.auction.server.services;

import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Response;
import com.auction.shared.models.AutoBidSettings; // Nhớ import model này

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {
    private static volatile AuctionManager instance;
    private final Map<String, ClientHandler> activeClients = new ConcurrentHashMap<>();
    
    // THÊM MỚI: Nơi lưu trữ cấu hình Auto-Bid của các Client
    private final Map<String, AutoBidSettings> autoBidders = new ConcurrentHashMap<>();

    private double currentHighestBid = 1240.00; 
    private final double minIncrement = 20.00;  
    private String currentHighestBidder = "None";

    private AuctionManager() {}

    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) instance = new AuctionManager();
            }
        }
        return instance;
    }

    // Hàm gọi khi người dùng Login thành công
    public void registerClient(String clientId, ClientHandler handler) {
        activeClients.put(clientId, handler);
        System.out.println("👥 [MANAGER] Đã định danh Client: " + clientId + " (Tổng online: " + activeClients.size() + ")");
    }

    public void removeClient(String clientId) {
        activeClients.remove(clientId);
        autoBidders.remove(clientId); // Xóa luôn auto-bid nếu họ thoát
    }

    public void broadcast(Response response) {
        for (ClientHandler client : activeClients.values()) {
            client.sendResponse(response);
        }
    }

    // THÊM MỚI: Đăng ký Auto-bid
    public void registerAutoBid(AutoBidSettings settings) {
        autoBidders.put(settings.getBidderId(), settings);
        System.out.println("⚙️ [MANAGER] Kích hoạt Auto-Bid cho: " + settings.getBidderId() + " (Max: $" + settings.getMaxPrice() + ")");
        // Quét ngay lập tức xem có cần tự động đặt giá luôn không
        new Thread(this::evaluateAutoBids).start(); 
    }

    public synchronized Response processBid(String bidderId, double amount, String payload) {
        double requiredMinBid = currentHighestBid + minIncrement;

        if (amount >= requiredMinBid) {
            currentHighestBid = amount;
            currentHighestBidder = bidderId;
            System.out.println("✅ [MANAGER] Giá mới: $" + amount + " từ " + bidderId);

            Response broadcastResp = new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS", "Có người đặt giá mới", payload);
            broadcast(broadcastResp);

            // THÊM MỚI: Sau khi có người đặt giá, kích hoạt luồng kiểm tra Auto-Bid ngầm
            new Thread(this::evaluateAutoBids).start();

            return new Response(MessageType.BID_SUCCESS, "SUCCESS", "Đặt giá thành công!", payload);
        } else {
            return new Response(MessageType.BID_ERROR, "FAIL", "Giá tối thiểu là $" + requiredMinBid, null);
        }
    }

    // THÊM MỚI: Logic Đấu giá tự động (Giải quyết bài toán 2 người cùng Auto-bid)
    private synchronized void evaluateAutoBids() {
        boolean bidChanged;
        do {
            bidChanged = false;
            AutoBidSettings bestCandidate = null;
            double requiredMinBid = currentHighestBid + minIncrement;

            // Tìm người có Auto-bid đủ sức trả giá tiếp theo
            for (AutoBidSettings ab : autoBidders.values()) {
                if (!ab.getBidderId().equals(currentHighestBidder) && ab.isActive()) {
                    if (ab.getMaxPrice() >= requiredMinBid) {
                        if (bestCandidate == null || ab.getMaxPrice() > bestCandidate.getMaxPrice()) {
                            bestCandidate = ab;
                        }
                    }
                }
            }

            // Nếu tìm thấy người nâng giá, Server tự động thay mặt họ đặt giá
            if (bestCandidate != null) {
                double nextBid = currentHighestBid + bestCandidate.getBidIncrement();
                if (nextBid > bestCandidate.getMaxPrice()) nextBid = bestCandidate.getMaxPrice();

                if (nextBid >= currentHighestBid + minIncrement) {
                    currentHighestBid = nextBid;
                    currentHighestBidder = bestCandidate.getBidderId();
                    System.out.println("🤖 [AUTO-BID] Tự động nâng giá lên: $" + currentHighestBid + " cho " + currentHighestBidder);
                    
                    Response broadcastResp = new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS", "Hệ thống tự động trả giá", null);
                    broadcast(broadcastResp);
                    bidChanged = true; // Tiếp tục vòng lặp xem có ai bật Auto-bid đè lại không
                }
            }
        } while (bidChanged);
    }
}