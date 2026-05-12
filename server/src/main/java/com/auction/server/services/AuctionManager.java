package com.auction.server.services;

import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.ItemDAOImpl;
import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Response;
import com.auction.shared.models.AutoBidSettings;
import com.auction.shared.models.BidTransaction;
import com.auction.shared.models.Item;
import com.google.gson.Gson;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionManager {
    private static volatile AuctionManager instance;
    private final ExecutorService autoBidThreadPool = Executors.newFixedThreadPool(10);
    private final Map<String, ClientHandler> activeClients = new ConcurrentHashMap<>();

    // Nơi lưu trữ cấu hình Auto-Bid của các Client
    private final Map<String, AutoBidSettings> autoBidders = new ConcurrentHashMap<>();

    // NÂNG CẤP: Thêm ItemDAO để làm việc với Database
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private String currentAuctionItemId = "ITEM-123"; // ID mặc định hoặc lấy từ config

    private double startingPrice = 1240.00; // Giá khởi điểm cố định
    private double currentHighestBid = 0.0; // Chưa ai đặt thì bằng 0
    private final double minIncrement = 20.00;
    private String currentHighestBidder = null; // null nghĩa là chưa có ai

    private AuctionManager() {
        // NÂNG CẤP: Khi vừa tạo Manager, load ngay giá từ Database lên RAM
        loadAuctionState();
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null)
                    instance = new AuctionManager();
            }
        }
        return instance;
    }

    // NÂNG CẤP: Hàm khôi phục "hiện trường" từ Database
    private void loadAuctionState() {
        Item item = itemDAO.getItemById(currentAuctionItemId);
        if (item != null) {
            this.startingPrice = item.getStartingPrice();
            this.currentHighestBid = item.getCurrentHighestBid();
            this.currentHighestBidder = item.getHighestBidderId();
            System.out.println("💾 [DATABASE] Đã khôi phục trạng thái phiên: $" + currentHighestBid + " (Người dẫn đầu: " + currentHighestBidder + ")");
        }
    }

    // Hàm gọi khi người dùng Login thành công
    public void registerClient(String clientId, ClientHandler handler) {
        activeClients.put(clientId, handler);
        System.out.println(
                "👥 [MANAGER] Đã định danh Client: " + clientId + " (Tổng online: " + activeClients.size() + ")");
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

    // Đăng ký Auto-bid
    // Phân biệt Giá khởi điểm và Giá hiện tại để Robot không bị oan
    public Response registerAutoBid(AutoBidSettings settings) {
        double requiredMinBid;

        // Logic "Vé vào cửa" mới: Kiểm tra xem đã có ai mở bát chưa?
        if (currentHighestBidder == null) {
            requiredMinBid = startingPrice;
        } else {
            requiredMinBid = currentHighestBid + minIncrement;
        }

        // Kiểm tra 1: Giá tối đa có đủ để tham gia vòng tiếp theo không?
        if (settings.getMaxPrice() < requiredMinBid) {
            return new Response(
                    MessageType.SETUP_AUTO_BID,
                    "FAIL",
                    "Giá tối đa ($" + settings.getMaxPrice() + ") phải lớn hơn hoặc bằng mức giá yêu cầu hiện tại ($"
                            + requiredMinBid + ")",
                    null);
        }

        // Kiểm tra 2: Bước giá tự động có tuân thủ quy định không?
        if (settings.getBidIncrement() < minIncrement) {
            return new Response(
                    MessageType.SETUP_AUTO_BID,
                    "FAIL",
                    "Bước giá tự động không được thấp hơn bước giá hệ thống ($" + minIncrement + ")",
                    null);
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
            requiredMinBid = startingPrice;
        } else {
            requiredMinBid = currentHighestBid + minIncrement;
        }

        // Trạm kiểm duyệt vé vào cửa
        if (amount >= requiredMinBid) {
            // NÂNG CẤP: Lưu vào Database trước khi cập nhật RAM để đảm bảo tính bền vững
            boolean dbUpdated = itemDAO.updateCurrentPrice(currentAuctionItemId, amount, bidderId);

            if (dbUpdated) {
                currentHighestBid = amount;
                currentHighestBidder = bidderId;
                System.out.println("[MANAGER] Giá mới: $" + amount + " từ " + bidderId);

                Response broadcastResp = new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS", "Có người đặt giá mới",
                        payload);
                broadcast(broadcastResp);

                // Kích hoạt luồng kiểm tra Auto-Bid ngầm
                autoBidThreadPool.submit(this::evaluateAutoBids);

                return new Response(MessageType.BID_SUCCESS, "SUCCESS", "Đặt giá thành công!", payload);
            } else {
                return new Response(MessageType.BID_ERROR, "FAIL", "Lỗi đồng bộ Database (có thể giá đã bị người khác đẩy lên cao hơn)", null);
            }
        } else {
            return new Response(MessageType.BID_ERROR, "FAIL", "Giá tối thiểu là $" + requiredMinBid, null);
        }
    }

    // Logic Đấu giá tự động (Giải quyết bài toán 2 người cùng Auto-bid)
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
                    
                    // NÂNG CẤP: Robot cũng phải lưu giá vào Database
                    boolean dbUpdated = itemDAO.updateCurrentPrice(currentAuctionItemId, nextBid, bestCandidate.getBidderId());
                    
                    if (dbUpdated) {
                        currentHighestBid = nextBid;
                        currentHighestBidder = bestCandidate.getBidderId();
                        System.out.println("[AUTO-BID] Tự động nâng giá lên: $" + currentHighestBid + " cho "
                                + currentHighestBidder);

                        // Tạo BidTransaction payload để client có thể hiển thị thông tin
                        BidTransaction autoBidTx = new BidTransaction(
                                "AUTO-" + System.currentTimeMillis(),
                                currentAuctionItemId,
                                currentHighestBidder,
                                currentHighestBid,
                                System.currentTimeMillis());
                        String autoBidPayload = new Gson().toJson(autoBidTx);

                        Response broadcastResp = new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS",
                                "Auto-bid placed", autoBidPayload);
                        broadcast(broadcastResp);
                        bidChanged = true; // Tiếp tục vòng lặp xem có ai bật Auto-bid đè lại không
                    }
                }
            }
        } while (bidChanged);
    }

    public Response getCurrentStatusResponse() {
        double displayPrice = (currentHighestBid > 0) ? currentHighestBid : startingPrice;
        String bidder = (currentHighestBidder != null) ? currentHighestBidder : "None";

        // Reuse BidTransaction to represent the current state
        BidTransaction statusTx = new BidTransaction("STATUS", currentAuctionItemId, bidder, displayPrice, System.currentTimeMillis());
        String payload = new Gson().toJson(statusTx);

        return new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS", "Current Auction Status", payload);
    }

    // Ham tat Thread neu ban dot ngot tat chuong trinh
    public void shutdown() {
        System.out.println("[AuctionManager] Đang dừng hệ thống Auto-Bid...");
        autoBidThreadPool.shutdown(); // Ngừng nhận lệnh đánh giá mới
        try {
            // Cho phép các trận "Ping-Pong" đang đánh dở có 3 giây để kết thúc
            if (!autoBidThreadPool.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS)) {
                autoBidThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            autoBidThreadPool.shutdownNow();
        }
    }
}