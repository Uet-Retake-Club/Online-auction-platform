package com.auction.server.services;

import com.auction.server.dao.BidTransactionDAO;
import com.auction.server.dao.BidTransactionDAOImpl;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.ItemDAOImpl;
import com.auction.server.dao.InvoiceDAO;
import com.auction.server.dao.InvoiceDAOImpl;
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
import java.util.List;

public class AuctionService {
    private static volatile AuctionService instance;

    // 3 Kẻ giúp việc
    private final SessionManager sessionManager;
    private final AutoBidEngine autoBidEngine;
    private final AuctionTimer auctionTimer;
    
    // Tương tác Database
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final BidTransactionDAO bidDAO = new BidTransactionDAOImpl();
    private final com.auction.server.dao.WalletDAO walletDAO = new com.auction.server.dao.WalletDAOImpl();
    private final com.auction.server.dao.AuctionDAO auctionDAO = new com.auction.server.dao.AuctionDAOImpl();
    private final com.auction.server.dao.UserDAO userDAO = new com.auction.server.dao.UserDAOImpl();
    private final com.auction.server.dao.InvoiceDAO invoiceDAO = new com.auction.server.dao.InvoiceDAOImpl();
    
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
        if ("true".equals(System.getProperty("testMode"))) {
            System.out.println(" [DATABASE] Test mode detected. Skipping loadAuctionState().");
            return;
        }
        Item item = itemDAO.getFirstOpenItem();
        if (item != null) {
            this.currentAuctionItemId = item.getId();
            this.startingPrice = item.getStartingPrice();
            this.currentHighestBid = item.getCurrentHighestBid();
            this.currentHighestBidder = item.getHighestBidderId();
            
            // Persist Auction session if not exists
            String auctionId = "AUC-" + item.getId();
            if (auctionDAO.getAuctionById(auctionId) == null) {
                com.auction.shared.models.User user = userDAO.getUserById(item.getSellerId());
                if (user != null) {
                    com.auction.shared.models.Seller seller = (user instanceof com.auction.shared.models.Seller)
                        ? (com.auction.shared.models.Seller) user
                        : new com.auction.shared.models.Seller(user.getId(), user.getUsername(), user.getEmail(), user.getStatus());
                    auctionDAO.addAuction(new com.auction.shared.models.Auction(auctionId, item, seller));
                    System.out.println(" [DATABASE] Created new persistent auction record: " + auctionId);
                }
            }

            System.out.println(" [DATABASE] State restored for item " + currentAuctionItemId + " : $" + currentHighestBid + " from SQLite.");

            // khoi phuc time sau khi khoi dong lai
            long currentTime = System.currentTimeMillis();
            if (item.getEndTime() > currentTime) {
                // Nếu thời gian kết thúc vẫn ở trong tương lai, bật lại đồng hồ đếm ngược!
                System.out.println(" [TIMER] Resuming countdown for item " + currentAuctionItemId);
                auctionTimer.scheduleAuctionEnd(item.getEndTime());
            } else {
                // Nếu server sập mà thời gian đã quá hạn, ép kết thúc luôn để chốt đơn
                System.out.println(" [TIMER] Item " + currentAuctionItemId + " is past end time. Ending now...");
                endAuction();
            }
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

        // 1. Kiểm tra số dư ví (Cumulative Stake check)
        double previousStake = bidDAO.getMaxBidAmount(bidderId, targetItemId);
        double deductionNeeded = amount - previousStake;
        double userBalance = walletDAO.getBalance(bidderId);

        if (userBalance < deductionNeeded) {
            return new Response(MessageType.BID_ERROR, "FAIL", 
                String.format("Số dư ví không đủ! Cần thêm $%.2f (Đã đặt: $%.2f)", deductionNeeded, previousStake), null);
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

            // 2. TRỪ TIỀN người đặt giá mới (Chỉ trừ phần chênh lệch)
            walletDAO.updateBalance(bidderId, -deductionNeeded);
            System.out.println("[WALLET] Cumulative Stake: Deducted additional $" + deductionNeeded + " from " + bidderId + " (Total: $" + amount + ")");

            // Log transaction to DB
            String txId = "BID-" + java.util.UUID.randomUUID().toString().substring(0, 8);
            BidTransaction tx = new BidTransaction(txId, currentAuctionItemId, bidderId, amount, System.currentTimeMillis());
            com.auction.shared.models.User user = userDAO.getUserById(bidderId);
            if (user != null) {
                tx.setBidderUsername(user.getUsername());
            }
            bidDAO.addTransaction(tx);

            currentHighestBid = amount;
            currentHighestBidder = bidderId;
            String responsePayload = new Gson().toJson(tx);
            broadcast(new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS", "New bid placed", responsePayload));
            autoBidEngine.triggerEvaluation();
            return new Response(MessageType.BID_SUCCESS, "SUCCESS", "Bid placed successfully!", responsePayload);
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

            // 2. TRỪ TIỀN người đặt giá mới (Chỉ trừ phần chênh lệch)
            walletDAO.updateBalance(bidderId, -deductionNeeded);
            System.out.println("[WALLET] Cumulative Stake: Deducted additional $" + deductionNeeded + " from " + bidderId + " (Total: $" + amount + ")");

            // Log transaction to DB
            String txId = "BID-" + java.util.UUID.randomUUID().toString().substring(0, 8);
            BidTransaction tx = new BidTransaction(txId, targetItemId, bidderId, amount, System.currentTimeMillis());
            com.auction.shared.models.User user = userDAO.getUserById(bidderId);
            if (user != null) {
                tx.setBidderUsername(user.getUsername());
            }
            bidDAO.addTransaction(tx);

            String responsePayload = new Gson().toJson(tx);
            broadcast(new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS", "New bid placed", responsePayload));
            return new Response(MessageType.BID_SUCCESS, "SUCCESS", "Bid placed successfully!", responsePayload);
        }
    }

    // Callback riêng cho luồng Robot
    public synchronized boolean processAutoBid(String bidderId, double nextBid) {
        if (currentAuctionItemId == null) return false;
        if (this.auctionStatus.equals("FINISHED")) return false;

        // 1. Kiểm tra số dư ví (Cumulative Stake check)
        double previousStake = bidDAO.getMaxBidAmount(bidderId, currentAuctionItemId);
        double deductionNeeded = nextBid - previousStake;
        double userBalance = walletDAO.getBalance(bidderId);

        if (userBalance < deductionNeeded) {
            System.err.println("[AUTO-BID] Insufficient funds for " + bidderId + " to bid $" + nextBid);
            return false;
        }

        boolean dbUpdated = itemDAO.updateCurrentPrice(currentAuctionItemId, nextBid, bidderId);
        if (dbUpdated) {
            // 2. TRỪ TIỀN (Chỉ trừ phần chênh lệch)
            walletDAO.updateBalance(bidderId, -deductionNeeded);
            System.out.println("[AUTO-BID] Cumulative Stake: Deducted additional $" + deductionNeeded + " from " + bidderId + " (Total: $" + nextBid + ")");

            currentHighestBid = nextBid;
            currentHighestBidder = bidderId;
            System.out.println("[AUTO-BID] Auto-bid placed: $" + currentHighestBid + " for " + bidderId);

            // Log transaction to DB
            String txId = "AUTO-" + java.util.UUID.randomUUID().toString().substring(0, 8);
            BidTransaction autoBidTx = new BidTransaction(txId, currentAuctionItemId, bidderId, nextBid, System.currentTimeMillis());
            com.auction.shared.models.User user = userDAO.getUserById(bidderId);
            if (user != null) {
                autoBidTx.setBidderUsername(user.getUsername());
            }
            bidDAO.addTransaction(autoBidTx);

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

            // 3. Hoàn tiền cho tất cả những người tham gia nhưng không thắng
            List<String> participants = bidDAO.getBiddersForItem(currentAuctionItemId);
            for (String pId : participants) {
                if (!pId.equals(currentHighestBidder)) {
                    double refundAmount = bidDAO.getMaxBidAmount(pId, currentAuctionItemId);
                    if (refundAmount > 0) {
                        walletDAO.updateBalance(pId, refundAmount);
                        System.out.println("[ESCROW] Refunded $" + refundAmount + " to " + pId + " (Non-winner)");
                    }
                }
            }

            // 4. Xác định người thắng và thông báo
            String winnerMsg;
            if (currentHighestBidder != null) {
                winnerMsg = "Người chiến thắng: " + currentHighestBidder + " ($" + currentHighestBid + ")";
                
                Item item = itemDAO.getItemById(currentAuctionItemId);
                String sellerId = (item != null) ? item.getSellerId() : "UNKNOWN_SELLER";
                String invoiceId = "INV-" + java.util.UUID.randomUUID().toString().substring(0, 8);
                String auctionId = "AUC-" + currentAuctionItemId; 
                
                // Initialize a PENDING invoice
                com.auction.shared.models.Invoice invoice = new com.auction.shared.models.Invoice(
                    invoiceId, auctionId, currentAuctionItemId,
                    currentHighestBidder, sellerId, currentHighestBid,
                    System.currentTimeMillis(), "PENDING"
                );
                
                if (invoiceDAO.createInvoice(invoice)) {
                    System.out.println("  [INVOICE] Created new invoice: " + invoiceId + " (Status: PENDING)");
                }
            } else {
                winnerMsg = "Phiên kết thúc mà không có ai đặt giá.";
            }
        }
    }

    public Response getCurrentStatusResponse() {
        List<BidTransaction> history = bidDAO.getHistoryByItem(currentAuctionItemId);
        String payload = new Gson().toJson(history);
        long endTime = 0;
        if (currentAuctionItemId != null) {
            Item item = itemDAO.getItemById(currentAuctionItemId);
            if (item != null) {
                endTime = item.getEndTime();
            }
        }
        return new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS", String.valueOf(endTime), payload);
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
        List<BidTransaction> history = bidDAO.getHistoryByItem(itemId);
        String payload = new Gson().toJson(history);
        return new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS", String.valueOf(item.getEndTime()), payload);
    }
    // --- PAYMENT PROCESSING ---
    public synchronized Response processPayment(String invoiceId, String userId) {
        com.auction.shared.models.Invoice invoice = invoiceDAO.getInvoiceById(invoiceId);
        if (invoice == null) return new Response(MessageType.BID_ERROR, "FAIL", "Invoice not found!", null);
        
        // Authorization check: Only the winning bidder can pay
        if (!invoice.getBidderId().equals(userId)) return new Response(MessageType.BID_ERROR, "FAIL", "Unauthorized payment attempt!", null);
        
        // Prevent double payment
        if (!invoice.getStatus().equals("PENDING")) return new Response(MessageType.BID_ERROR, "FAIL", "Invoice already processed (" + invoice.getStatus() + ")!", null);
        
        // 1. Update DB status to PAID for both Invoice and Item
        if (invoiceDAO.updateInvoiceStatus(invoiceId, "PAID") && itemDAO.updateStatus(invoice.getItemId(), "PAID")) {
            // 2. Transfer funds to the Seller's wallet (Bidder's funds were deducted during bidding)
            walletDAO.updateBalance(invoice.getSellerId(), invoice.getFinalPrice());
            System.out.println("  [PAYMENT] Transferred $" + invoice.getFinalPrice() + " to Seller: " + invoice.getSellerId());
            
            return new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS", "Payment successful!", invoiceId);
        }
        return new Response(MessageType.BID_ERROR, "ERROR", "Database error during payment!", null);
    }

    // --- CANCELLATION PROCESSING ---
    public synchronized Response processCancellation(String invoiceId, String userId) {
        com.auction.shared.models.Invoice invoice = invoiceDAO.getInvoiceById(invoiceId);
        if (invoice == null || !invoice.getStatus().equals("PENDING")) {
            return new Response(MessageType.BID_ERROR, "FAIL", "Cannot cancel this invoice!", null);
        }
        
        // 1. Update DB status to CANCELED
        invoiceDAO.updateInvoiceStatus(invoiceId, "CANCELED");
        itemDAO.updateStatus(invoice.getItemId(), "CANCELED");
        
        // 2. Refund the escrowed amount to the canceled winner
        // (If your team's rule is to penalize/forfeit the deposit, remove this updateBalance call)
        walletDAO.updateBalance(invoice.getBidderId(), invoice.getFinalPrice());
        System.out.println("  [CANCELLATION] Invoice " + invoiceId + " canceled. Refunded bidder.");
        
        return new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS", "Invoice canceled successfully!", invoiceId);
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
