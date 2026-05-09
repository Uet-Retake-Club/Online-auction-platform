package com.auction.client.services;

import com.auction.client.utils.UserSession;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.shared.models.AutoBidSettings;
import com.auction.shared.models.BidTransaction;
import com.google.gson.Gson;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Lớp trung gian giữa UI và Mạng.
 * Kế thừa logic UI cũ nhưng dữ liệu lấy từ Socket thật.
 */
public class BidService implements NetworkClientService.ServerMessageListener {

    private static BidService instance;
    private double currentBidAmount = 0.0;
    private double minimumIncrement = 20.00;
    private boolean isAuctionOpen = true;
    
    private final List<BidTransaction> bidHistory = new ArrayList<>();
    private final Gson gson = new Gson();
    
    private Consumer<Double> onPriceUpdated;
    private Consumer<BidTransaction> onNewBid;
    private Consumer<String> onPriceChangeNotification; // Toast thông báo thay đổi giá
    private Consumer<Response> onAutoBidResult;  // Kết quả đăng ký auto-bid (SUCCESS/FAIL)
    private Consumer<String> onBidError;         // Thông báo lỗi đặt giá

    private BidService() {
        // Đăng ký làm Observer để nhận thông báo từ Socket
        NetworkClientService.getInstance().addListener(this);
    }

    public static BidService getInstance() {
        if (instance == null) {
        synchronized(BidService.class) {
            if (instance == null) {
                 instance = new BidService();
            }
        }
        }
        return instance;
    }

    public void setCallbacks(Consumer<Double> onPriceUpdated, Consumer<BidTransaction> onNewBid) {
        this.onPriceUpdated = onPriceUpdated;
        this.onNewBid = onNewBid;
    }

    public void setOnPriceChangeNotification(Consumer<String> callback) {
        this.onPriceChangeNotification = callback;
    }

    public void setOnAutoBidResult(Consumer<Response> callback) {
        this.onAutoBidResult = callback;
    }

    public void setOnBidError(Consumer<String> callback) {
        this.onBidError = callback;
    }

    public double getCurrentBidAmount() { return currentBidAmount; }
    public double getMinimumIncrement() { return minimumIncrement; }

    public void setAuctionClosed() {
        this.isAuctionOpen = false;
    }

    // Xử lý gửi lệnh đặt giá lên Server
    public String placeBid(String bidderId, String auctionId, double amount) {
        if (!isAuctionOpen) return "Phiên đấu giá đã đóng.";
        
        // Tạo giao dịch và bọc thành JSON
        BidTransaction transaction = new BidTransaction("", auctionId, bidderId, amount, System.currentTimeMillis());
        String payload = gson.toJson(transaction);
        
        Request req = new Request(MessageType.PLACE_BID, UserSession.getInstance().getUsername(), payload);
        NetworkClientService.getInstance().sendRequest(req);
        
        return null; // Trả về null tức là đã gửi đi, chờ Server xác nhận
    }
    
    // Xử lý gửi lệnh Auto-bid lên Server
    public String setupAutoBid(String bidderId, String auctionId, double maxPrice, double bidIncrement , boolean aggressiveMode) {
        if (!isAuctionOpen) return "Phiên đấu giá đã đóng.";
        
        AutoBidSettings settings = new AutoBidSettings(bidderId, auctionId, maxPrice, bidIncrement,aggressiveMode);
        String payload = gson.toJson(settings);
        
        Request req = new Request(MessageType.SETUP_AUTO_BID, UserSession.getInstance().getUsername(), payload);
        NetworkClientService.getInstance().sendRequest(req);
        
        return null;
    }

    /**
     * Gửi yêu cầu lấy trạng thái giá hiện tại từ Server.
     */
    public void requestStatus() {
        Request req = new Request(MessageType.GET_STATUS, UserSession.getInstance().getUsername(), "");
        NetworkClientService.getInstance().sendRequest(req);
    }

    // ĐÂY LÀ NƠI NHẬN DỮ LIỆU TỪ SOCKET VÀ BƠM LÊN GIAO DIỆN
    @Override
    public void onMessageReceived(Response response) {
        if (response.getType() == MessageType.NEW_BID_BROADCAST || response.getType() == MessageType.BID_SUCCESS) {
            
            // Kiểm tra payload tránh null
            if (response.getPayload() == null || response.getPayload().isEmpty()) {
                return;
            }

            // Giải nén JSON thành Object
            BidTransaction newBid = gson.fromJson(response.getPayload(), BidTransaction.class);
            
            this.currentBidAmount = newBid.getBidAmount();
            bidHistory.add(newBid);
            
            // Ép luồng chạy trên JavaFX Thread để update giao diện an toàn
            Platform.runLater(() -> {
                if (onPriceUpdated != null) onPriceUpdated.accept(currentBidAmount);
                if (onNewBid != null) onNewBid.accept(newBid);

                // Hiển thị thông báo thay đổi giá
                if (onPriceChangeNotification != null) {
                    String msg = String.format("Price updated: $%.2f by %s",
                            newBid.getBidAmount(), newBid.getBidderId());
                    onPriceChangeNotification.accept(msg);
                }
            });
            
        } else if (response.getType() == MessageType.SETUP_AUTO_BID) {
            // Phản hồi từ server khi đăng ký auto-bid
            Platform.runLater(() -> {
                if (onAutoBidResult != null) {
                    onAutoBidResult.accept(response);
                }
            });

        } else if (response.getType() == MessageType.AUCTION_ENDED) {
            this.isAuctionOpen = false;

        } else if (response.getType() == MessageType.BID_ERROR) {
            System.err.println("Lỗi đặt giá: " + response.getMessage());
            Platform.runLater(() -> {
                if (onBidError != null) {
                    onBidError.accept(response.getMessage());
                }
            });
        }
    }
}