package com.auction.server.services;

import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.ItemDAOImpl;
import com.auction.server.network.ClientHandler;
import com.auction.server.services.core.AuctionTimer;
import com.auction.server.services.core.AutoBidEngine;
import com.auction.server.services.core.SessionManager;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Response;
import com.auction.shared.models.AutoBidSettings;
import com.auction.shared.models.BidTransaction;
import com.auction.shared.models.Item;
import com.google.gson.Gson;

public class AuctionService {
    private static volatile AuctionService instance;

    // 3 Kẻ giúp việc
    private final SessionManager sessionManager;
    private final AutoBidEngine autoBidEngine;
    private final AuctionTimer auctionTimer;
    
    // Tương tác Database
    private final ItemDAO itemDAO = new ItemDAOImpl();
    
    private String currentAuctionItemId = null;
    private double startingPrice = 1240.00;
    private double currentHighestBid = 0.0;
    private final double minIncrement = 20.00;
    private String currentHighestBidder = null;
    private String auctionStatus = "OPEN"; 

    private AuctionService() {
        this.sessionManager = new SessionManager();
        this.autoBidEngine = new AutoBidEngine(this); // Truyền 'this' để Engine đọc biến
        this.auctionTimer = new AuctionTimer(this);
        loadAuctionState();
    }

    public static AuctionService getInstance() {
        if (instance == null) {
            synchronized (AuctionService.class) {
                if (instance == null) instance = new AuctionService();
            }
        }
        return instance;
    }

    // --- GETTERS CHO AUTO-BID ENGINE ---
    public double getCurrentHighestBid() { return currentHighestBid; }
    public String getCurrentHighestBidder() { return currentHighestBidder; }
    public double getMinIncrement() { return minIncrement; }

    private void loadAuctionState() {
        Item item = itemDAO.getFirstOpenItem();
        if (item != null) {
            this.currentAuctionItemId = item.getId();
            this.startingPrice = item.getStartingPrice();
            this.currentHighestBid = item.getCurrentHighestBid();
            this.currentHighestBidder = item.getHighestBidderId();
            System.out.println(" [DATABASE] State restored for item " + currentAuctionItemId + " : $" + currentHighestBid + " from SQLite.");
            
            // Nếu có dữ liệu endTime, kích hoạt đồng hồ luôn:
            // auctionTimer.scheduleAuctionEnd(item.getEndTime());
        } else {
            System.out.println(" [DATABASE] No OPEN items found. Waiting for items to be added.");
        }
    }

    // --- GIAO TIẾP MẠNG ---
    public void registerClient(String clientId, ClientHandler handler) {
        sessionManager.registerClient(clientId, handler);
    }
    public void removeClient(String clientId) {
        sessionManager.removeClient(clientId);
        autoBidEngine.removeAutoBidder(clientId);
    }
    public void broadcast(Response response) {
        sessionManager.broadcast(response);
    }

    // --- LOGIC LUẬT CHƠI ---
    public Response registerAutoBid(AutoBidSettings settings) {
        double requiredMinBid = (currentHighestBidder == null) ? startingPrice : currentHighestBid + minIncrement;
        if (settings.getMaxPrice() < requiredMinBid) return new Response(MessageType.SETUP_AUTO_BID, "FAIL", "Giá tối đa không đủ", null);
        if (settings.getBidIncrement() < minIncrement) return new Response(MessageType.SETUP_AUTO_BID, "FAIL", "Bước giá quá thấp", null);

        autoBidEngine.addAutoBidder(settings);
        autoBidEngine.triggerEvaluation();
        return new Response(MessageType.SETUP_AUTO_BID, "SUCCESS", "Cấu hình Auto-Bid kích hoạt!", null);
    }

    public synchronized Response processBid(String bidderId, double amount, String payload) {
        if (currentAuctionItemId == null) {
            return new Response(MessageType.BID_ERROR, "FAIL", "No active auction session found!", null);
        }

        // Parse the target item ID from the bid transaction payload (guard against plain-string payloads)
        BidTransaction bidTx = null;
        try {
            bidTx = new Gson().fromJson(payload, BidTransaction.class);
        } catch (Exception ignored) { /* non-JSON payload: fall back to active item */ }
        String targetItemId = (bidTx != null && bidTx.getItemId() != null)
            ? bidTx.getItemId() : currentAuctionItemId;

        if (this.auctionStatus.equals("FINISHED") && targetItemId.equals(currentAuctionItemId)) {
            return new Response(MessageType.BID_ERROR, "FAIL", "Auction has already ended!", null);
        }

        if (targetItemId.equals(currentAuctionItemId)) {
            // ── Active auction item: use fast in-memory path ──
            double requiredMinBid = (currentHighestBidder == null)
                ? startingPrice : currentHighestBid + minIncrement;
            if (amount < requiredMinBid) {
                return new Response(MessageType.BID_ERROR, "FAIL",
                    String.format("Minimum bid is $%.2f", requiredMinBid), null);
            }
            boolean dbUpdated = itemDAO.updateCurrentPrice(currentAuctionItemId, amount, bidderId);
            if (!dbUpdated) {
                return new Response(MessageType.BID_ERROR, "FAIL", "Database sync error", null);
            }
            currentHighestBid = amount;
            currentHighestBidder = bidderId;
            System.out.println("[MANAGER] New price: $" + amount + " from " + bidderId
                + " on item " + targetItemId);
            broadcast(new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS", "New bid placed", payload));
            autoBidEngine.triggerEvaluation();
            return new Response(MessageType.BID_SUCCESS, "SUCCESS", "Bid placed successfully!", payload);
        } else {
            // ── Non-active item: read current price from DB ──
            Item item = itemDAO.getItemById(targetItemId);
            if (item == null) {
                return new Response(MessageType.BID_ERROR, "FAIL", "Item not found: " + targetItemId, null);
            }
            if ("FINISHED".equals(item.getStatus())) {
                return new Response(MessageType.BID_ERROR, "FAIL", "Auction for this item has ended!", null);
            }
            double currentItemPrice = item.getCurrentHighestBid() > 0
                ? item.getCurrentHighestBid() : item.getStartingPrice();
            double requiredMinBid = item.getHighestBidderId() == null
                ? item.getStartingPrice() : currentItemPrice + minIncrement;
            if (amount < requiredMinBid) {
                return new Response(MessageType.BID_ERROR, "FAIL",
                    String.format("Minimum bid is $%.2f", requiredMinBid), null);
            }
            boolean dbUpdated = itemDAO.updateCurrentPrice(targetItemId, amount, bidderId);
            if (!dbUpdated) {
                return new Response(MessageType.BID_ERROR, "FAIL", "Database sync error", null);
            }
            System.out.println("[MANAGER] New price: $" + amount + " from " + bidderId
                + " on item " + targetItemId);
            broadcast(new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS", "New bid placed", payload));
            return new Response(MessageType.BID_SUCCESS, "SUCCESS", "Bid placed successfully!", payload);
        }
    }

    // Callback riêng cho luồng Robot
    public synchronized boolean processAutoBid(String bidderId, double nextBid) {
        if (currentAuctionItemId == null) return false;
        if (this.auctionStatus.equals("FINISHED")) return false;

        boolean dbUpdated = itemDAO.updateCurrentPrice(currentAuctionItemId, nextBid, bidderId);
        if (dbUpdated) {
            currentHighestBid = nextBid;
            currentHighestBidder = bidderId;
            System.out.println("[AUTO-BID] Auto-bid placed: $" + currentHighestBid + " for " + bidderId);

            BidTransaction autoBidTx = new BidTransaction("AUTO-" + System.currentTimeMillis(), currentAuctionItemId, bidderId, nextBid, System.currentTimeMillis());
            String autoBidPayload = new Gson().toJson(autoBidTx);
            broadcast(new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS", "Auto-bid placed", autoBidPayload));
            return true;
        }
        return false;
    }

    public synchronized void endAuction() {
    if (currentAuctionItemId == null) return;
    if (this.auctionStatus.equals("FINISHED")) return;
    
    // 1. Cập nhật trạng thái xuống SQLite trước
    boolean success = itemDAO.updateStatus(currentAuctionItemId, "FINISHED");
    
    if (success) {
        this.auctionStatus = "FINISHED";
        System.out.println(" [MANAGER] AUCTION ENDED!");
        
        // 2. Tắt hệ thống Robot
        autoBidEngine.shutdown();

        // 3. Xác định người thắng và thông báo
        String winnerMsg = (currentHighestBidder != null) 
            ? "Người chiến thắng: " + currentHighestBidder + " ($" + currentHighestBid + ")" 
            : "Phiên kết thúc mà không có ai đặt giá.";
            
        broadcast(new Response(MessageType.AUCTION_ENDED, "SUCCESS", winnerMsg, null));
    } else {
        System.err.println(" [ERROR] Database rejected auction closure!");
    }
}

    public Response getCurrentStatusResponse() {
        double displayPrice = (currentHighestBid > 0) ? currentHighestBid : startingPrice;
        String bidder = (currentHighestBidder != null) ? currentHighestBidder : "None";
        String payload = new Gson().toJson(new BidTransaction(
            "STATUS", currentAuctionItemId, bidder, displayPrice, System.currentTimeMillis()));
        return new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS", "Current Status", payload);
    }

    /**
     * Returns the status for a specific item by ID.
     * If the item is the active auction, returns live in-memory state.
     * Otherwise, fetches current state from the database.
     */
    public Response getItemStatusResponse(String itemId) {
        if (itemId != null && itemId.equals(currentAuctionItemId)) {
            return getCurrentStatusResponse();
        }
        // Fetch from DB for non-active items
        Item item = itemDAO.getItemById(itemId);
        if (item == null) {
            return new Response(MessageType.BID_ERROR, "FAIL", "Item not found: " + itemId, null);
        }
        double displayPrice = item.getCurrentHighestBid() > 0
            ? item.getCurrentHighestBid() : item.getStartingPrice();
        String bidder = item.getHighestBidderId() != null ? item.getHighestBidderId() : "None";
        String payload = new Gson().toJson(new BidTransaction(
            "STATUS", itemId, bidder, displayPrice, System.currentTimeMillis()));
        return new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS", "Item Status", payload);
    }

    // Nhạc trưởng gọi một tiếng, đàn em tự động dọn dẹp (Facade Pattern)
    public void shutdown() {
        System.out.println(" [AuctionService] Initiating safe Server shutdown...");
        sessionManager.shutdown();
        auctionTimer.shutdown();
        autoBidEngine.shutdown();
        System.out.println(" [AuctionService] Cleanup complete. Server fully shut down!");
    }
}

// Waiting Database InvoiceDao va logic thanh toan de hoan thanh PAID/CANCELED