package com.auction.server.services;

import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Response;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {
    private static volatile AuctionManager instance;
    private final Map<String, ClientHandler> activeClients = new ConcurrentHashMap<>();

    // --- CÁC BIẾN LƯU TRẠNG THÁI ĐẤU GIÁ ---
    private double currentHighestBid = 1240.00; // Giả sử giá khởi điểm
    private final double minIncrement = 20.00;  // Bước giá tối thiểu
    private String currentHighestBidder = "None";

    private AuctionManager() {}

    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    public void registerClient(String clientId, ClientHandler handler) {
        activeClients.put(clientId, handler);
    }

    public void removeClient(String clientId) {
        activeClients.remove(clientId);
    }

    public void broadcast(Response response) {
        for (ClientHandler client : activeClients.values()) {
            client.sendResponse(response);
        }
    }

    // --- LOGIC XỬ LÝ ĐẶT GIÁ (XÓA TODO) ---
    // Từ khóa "synchronized" cực kỳ quan trọng: Khóa hàm này lại, luồng nào đến trước xử lý trước!
    public synchronized Response processBid(String bidderId, double amount, String payload) {
        double requiredMinBid = currentHighestBid + minIncrement;

        if (amount >= requiredMinBid) {
            // 1. Cập nhật giá mới hợp lệ
            currentHighestBid = amount;
            currentHighestBidder = bidderId;
            System.out.println("✅ [MANAGER] Giá mới được thiết lập: $" + amount + " bởi " + bidderId);

            // 2. Tạo Response để Broadcast (thông báo) cho TẤT CẢ mọi người
            Response broadcastResp = new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS", "Có người vừa đặt giá mới", payload);
            broadcast(broadcastResp);

            // 3. Trả kết quả thành công riêng cho người vừa đặt giá
            return new Response(MessageType.BID_SUCCESS, "SUCCESS", "Bạn đã đặt giá thành công!", payload);
        } else {
            // Giá quá thấp, trả về lỗi cho riêng người đặt
            return new Response(MessageType.BID_ERROR, "FAIL", "Giá phải lớn hơn hoặc bằng $" + requiredMinBid, null);
        }
    }
}