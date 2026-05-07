package com.auction.server.services;

import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Response;
import com.auction.shared.models.AutoBidSettings; // Nhớ import model này

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionManager {
    private static volatile AuctionManager instance;
    private final ExecutorService autoBidThreadPool = Executors.newFixedThreadPool(10);
    private final Map<String, ClientHandler> activeClients = new ConcurrentHashMap<>();
    
    // THÊM MỚI: Nơi lưu trữ cấu hình Auto-Bid của các Client
    private final Map<String, AutoBidSettings> autoBidders = new ConcurrentHashMap<>();

    private final double startingPrice = 1240.00; // Giá khởi điểm cố định
    private double currentHighestBid = 0.0;       // Chưa ai đặt thì bằng 0
    private final double minIncrement = 20.00;
    private String currentHighestBidder = null;   // null nghĩa là chưa có ai
    

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
    // NÂNG CẤP: Chuyển sang trả về Response để ClientHandler có thể báo lỗi
// THÊM MỚI: Đăng ký Auto-bid
// NÂNG CẤP: Phân biệt Giá khởi điểm và Giá hiện tại để Robot không bị oan
public Response registerAutoBid(AutoBidSettings settings) {
    double requiredMinBid;

    // Logic "Vé vào cửa" mới: Kiểm tra xem đã có ai mở bát chưa?
    if (currentHighestBidder == null) {
        // Nếu Server vừa bật, chưa ai đặt giá -> Vé vào cửa chỉ là Giá khởi điểm (1240.00)
        requiredMinBid = startingPrice; 
    } else {
        // Nếu đã có người dẫn đầu -> Vé vào cửa là Giá hiện tại + Bước giá (20.00)
        requiredMinBid = currentHighestBid + minIncrement;
    }

    // Kiểm tra 1: Giá tối đa có đủ để tham gia vòng tiếp theo không?
    if (settings.getMaxPrice() < requiredMinBid) {
        return new Response(
            MessageType.SETUP_AUTO_BID, 
            "FAIL", 
            "Giá tối đa ($" + settings.getMaxPrice() + ") phải lớn hơn hoặc bằng mức giá yêu cầu hiện tại ($" + requiredMinBid + ")", 
            null
        );
    }

    // Kiểm tra 2: Bước giá tự động có tuân thủ quy định không?
    if (settings.getBidIncrement() < minIncrement) {
        return new Response(
            MessageType.SETUP_AUTO_BID, 
            "FAIL", 
            "Bước giá tự động không được thấp hơn bước giá hệ thống ($" + minIncrement + ")", 
            null
        );
    }

    // Nếu vượt qua các bài kiểm tra, tiến hành lưu cấu hình
    autoBidders.put(settings.getBidderId(), settings);
    System.out.println("⚙️ [MANAGER] Kích hoạt Auto-Bid thành công cho: " + settings.getBidderId());

    // Kích hoạt luồng kiểm tra để tự nâng giá ngay nếu cần
    autoBidThreadPool.submit(this::evaluateAutoBids);

    return new Response(MessageType.SETUP_AUTO_BID, "SUCCESS", "Cấu hình Auto-Bid đã được kích hoạt!", null);
}


    public synchronized Response processBid(String bidderId, double amount, String payload) {
    double requiredMinBid;

    // ĐỒNG BỘ LUẬT CHƠI: Kiểm tra xem phiên đấu giá đã có ai "mở bát" chưa?
    if (currentHighestBidder == null) { 
        // Nếu là người đầu tiên đặt giá, vé vào cửa chỉ là Giá khởi điểm (1240.00)
        requiredMinBid = startingPrice; 
    } else {
        // Nếu từ người thứ 2 trở đi, vé vào cửa = Giá hiện tại + Bước giá tối thiểu
        requiredMinBid = currentHighestBid + minIncrement;
    }

    // Trạm kiểm duyệt vé vào cửa
    if (amount >= requiredMinBid) {
        currentHighestBid = amount;
        currentHighestBidder = bidderId;
        System.out.println("✅ [MANAGER] Giá mới: $" + amount + " từ " + bidderId);

        Response broadcastResp = new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS", "Có người đặt giá mới", payload);
        broadcast(broadcastResp);

        // Kích hoạt luồng kiểm tra Auto-Bid ngầm
        autoBidThreadPool.submit(this::evaluateAutoBids);

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
               // BƯỚC 1: XÁC ĐỊNH CHIẾN THUẬT - Đây là đoạn code mới vẹn cả đôi đường
                    double step = bestCandidate.isAggressiveMode() 
                                ? bestCandidate.getBidIncrement() 
                                : minIncrement;

                    // BƯỚC 2: TÍNH TOÁN GIÁ TIẾP THEO
                    double nextBid = currentHighestBid + step;

                    // BƯỚC 3: CẮT NGỌN (Bảo vệ ví tiền - giữ nguyên logic cũ)
                    if (nextBid > bestCandidate.getMaxPrice()) {
                        nextBid = bestCandidate.getMaxPrice();
                    }

                    // BƯỚC 4: KIỂM TRA ĐIỀU KIỆN ĐẶT GIÁ (Giữ nguyên logic cũ)
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