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
import com.auction.shared.models.AuctionState;
import com.auction.shared.models.AutoBidSettings;
import com.auction.shared.models.BidTransaction;
import com.auction.shared.models.Item;
import com.auction.shared.utils.BidIncrementPolicy;
import com.google.gson.Gson;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    /**
     * Multi-item auction state map.
     *
     * <p>Key = itemId, Value = live in-memory state for that item's auction.
     *
     * <p>Replaces the five flat fields that previously handled a single global session:
     * {@code currentAuctionItemId}, {@code startingPrice}, {@code currentHighestBid},
     * {@code currentHighestBidder}, and {@code auctionStatus}. Any item stored here is
     * automatically eligible for auto-bidding regardless of its product type.
     */
    private final Map<String, AuctionState> activeAuctions = new ConcurrentHashMap<>();

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

    /**
     * Returns the live auction state for the given item, or {@code null} if the item is not
     * currently tracked as an active auction.
     *
     * <p>This is the primary method used by {@link AutoBidEngine} to read per-item state
     * without any type check or product-category switch.
     *
     * @param itemId the item whose state to retrieve
     * @return the {@link AuctionState} for this item, or {@code null}
     */
    public AuctionState getAuctionState(String itemId) {
        return activeAuctions.get(itemId);
    }

    /** Returns the current highest bid for the given item (0 if not active). */
    public double getCurrentHighestBid(String itemId) {
        AuctionState state = activeAuctions.get(itemId);
        return state != null ? state.getCurrentHighestBid() : 0.0;
    }

    /** Returns the current highest bidder for the given item (null if no bids). */
    public String getCurrentHighestBidder(String itemId) {
        AuctionState state = activeAuctions.get(itemId);
        return state != null ? state.getCurrentHighestBidder() : null;
    }

    /** Returns the per-item minimum bid increment. */
    public double getMinIncrement(String itemId) {
        AuctionState state = activeAuctions.get(itemId);
        return state != null ? state.getMinIncrement() : AuctionState.computeMinIncrement(0);
    }

    // --- BACKWARD-COMPAT GETTERS (delegate to first active item) ---
    // Used by tests that were written before multi-item support.
    // These will be gradually phased out.

    /** @deprecated Use {@link #getCurrentHighestBid(String)} instead. */
    @Deprecated
    public double getCurrentHighestBid() {
        return activeAuctions.values().stream()
                .mapToDouble(AuctionState::getCurrentHighestBid)
                .findFirst().orElse(0.0);
    }

    /** @deprecated Use {@link #getCurrentHighestBidder(String)} instead. */
    @Deprecated
    public String getCurrentHighestBidder() {
        return activeAuctions.values().stream()
                .map(AuctionState::getCurrentHighestBidder)
                .findFirst().orElse(null);
    }

    /** @deprecated Use {@link #getMinIncrement(String)} instead. */
    @Deprecated
    public double getMinIncrement() {
        return activeAuctions.values().stream()
                .mapToDouble(AuctionState::getMinIncrement)
                .findFirst().orElse(BidIncrementPolicy.calculate(0));
    }

    /**
     * Loads all OPEN auction items from the database at startup and registers them for
     * auto-bidding.
     *
     * <p>Previously only the first OPEN item was loaded, meaning any subsequent items
     * were invisible to the auto-bid engine. Now all OPEN items are loaded so the server
     * can resume correctly after a restart regardless of how many concurrent auctions
     * were running.
     */
    private void loadAuctionState() {
        if ("true".equals(System.getProperty("testMode"))) {
            System.out.println(" [DATABASE] Test mode detected. Skipping loadAuctionState().");
            return;
        }

        List<Item> openItems = itemDAO.getAllItems();
        if (openItems.isEmpty()) {
            System.out.println(" [DATABASE] No OPEN items found. Waiting for items to be added.");
            return;
        }

        for (Item item : openItems) {
            registerItemAsActiveAuction(item);
        }
        System.out.println(" [DATABASE] Loaded " + openItems.size() + " active auction(s).");
    }

    /**
     * Registers an item as an active auction: adds it to the in-memory map and schedules
     * its end timer.
     *
     * <p>This is called both during startup (for existing OPEN items) and at runtime (when
     * a new item is posted via {@code CreateItemHandler}). Because registration is generic —
     * it only needs an {@link Item} — any product type is automatically eligible for
     * auto-bidding without any additional code change.
     *
     * @param item the item to activate
     */
    public synchronized void registerItemAsActiveAuction(Item item) {
        if (activeAuctions.containsKey(item.getId())) {
            System.out.println(" [AuctionService] Item " + item.getId() + " already active.");
            return;
        }

        AuctionState state = new AuctionState(
                item.getId(),
                item.getStartingPrice(),
                item.getCurrentHighestBid() > 0 ? item.getCurrentHighestBid() : item.getStartingPrice(),
                item.getHighestBidderId());
        activeAuctions.put(item.getId(), state);

        // Persist Auction session record if not already present
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

        // Schedule the end timer
        long currentTime = System.currentTimeMillis();
        if (item.getEndTime() > currentTime) {
            System.out.println(" [TIMER] Scheduling end for item " + item.getId());
            auctionTimer.scheduleAuctionEnd(item.getId(), item.getEndTime());
        } else {
            System.out.println(" [TIMER] Item " + item.getId() + " is past end time. Ending now...");
            endAuction(item.getId());
        }

        System.out.println(" [AuctionService] Item " + item.getId()
                + " registered as active auction. MinIncrement=$" + state.getMinIncrement());
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
    public void sendToClient(String clientId, Response response) {
        sessionManager.sendToClient(clientId, response);
    }

    // --- LOGIC LUẬT CHƠI ---
    public Response registerAutoBid(AutoBidSettings settings) {
        String itemId = settings.getItemId();
        AuctionState state = activeAuctions.get(itemId);

        if (state == null) {
            return new Response(MessageType.SETUP_AUTO_BID, "FAIL",
                    "No active auction found for item: " + itemId, null);
        }

        double currentBid = state.getCurrentHighestBid();
        // When no bids have been placed yet, use the starting price as the
        // tier reference so the increment floor is sensible from auction open.
        double refPrice = (state.getCurrentHighestBidder() == null)
                ? state.getStartingPrice()
                : currentBid;
        double requiredMinBid = (state.getCurrentHighestBidder() == null)
                ? state.getStartingPrice()
                : BidIncrementPolicy.minNextBid(currentBid);

        if (settings.getMaxPrice() < requiredMinBid) {
            return new Response(MessageType.SETUP_AUTO_BID, "FAIL", "Giá tối đa không đủ", null);
        }
        double floor = BidIncrementPolicy.calculate(refPrice);
        if (!BidIncrementPolicy.isValidIncrement(refPrice, settings.getBidIncrement())) {
            return new Response(MessageType.SETUP_AUTO_BID, "FAIL",
                    String.format("Bước giá quá thấp (tối thiểu $%.0f)", floor), null);
        }

        autoBidEngine.addAutoBidder(settings);
        autoBidEngine.triggerEvaluation(itemId);
        return new Response(MessageType.SETUP_AUTO_BID, "SUCCESS", "Cấu hình Auto-Bid kích hoạt!", null);
    }

    public synchronized Response processBid(String bidderId, double amount, String payload) {
        // Parse the target item ID from the bid transaction payload
        String targetItemId = null;
        try {
            com.google.gson.JsonObject obj = new Gson().fromJson(payload, com.google.gson.JsonObject.class);
            if (obj != null && obj.has("itemId") && !obj.get("itemId").isJsonNull()) {
                targetItemId = obj.get("itemId").getAsString();
            }
        } catch (Exception ignored) { /* non-JSON payload */ }

        if (targetItemId == null || targetItemId.isEmpty()) {
            return new Response(MessageType.BID_ERROR, "FAIL", "No target item specified in bid!", null);
        }

        AuctionState state = activeAuctions.get(targetItemId);
        if (state == null) {
            return new Response(MessageType.BID_ERROR, "FAIL",
                    "No active auction found for item: " + targetItemId, null);
        }
        if ("FINISHED".equals(state.getStatus())) {
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

        // 2. Validate minimum bid — floor recalculated from current price each time
        double currentBid = state.getCurrentHighestBid();
        double requiredMinBid = (state.getCurrentHighestBidder() == null)
                ? state.getStartingPrice()
                : BidIncrementPolicy.minNextBid(currentBid);
        if (amount < requiredMinBid) {
            return new Response(MessageType.BID_ERROR, "FAIL",
                String.format("Minimum bid is $%.2f", requiredMinBid), null);
        }

        // 3. Persist to DB
        boolean dbUpdated = itemDAO.updateCurrentPrice(targetItemId, amount, bidderId);
        if (!dbUpdated) {
            return new Response(MessageType.BID_ERROR, "FAIL", "Database sync error", null);
        }

        // 4. Deduct wallet (only the incremental difference)
        walletDAO.updateBalance(bidderId, -deductionNeeded);
        System.out.println("[WALLET] Cumulative Stake: Deducted additional $" + deductionNeeded
                + " from " + bidderId + " (Total: $" + amount + ")");

        // 5. Log transaction
        String txId = "BID-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        BidTransaction tx = new BidTransaction(txId, targetItemId, bidderId, amount, System.currentTimeMillis());
        com.auction.shared.models.User user = userDAO.getUserById(bidderId);
        if (user != null) {
            tx.setBidderUsername(user.getUsername());
        }
        bidDAO.addTransaction(tx);

        // 6. Update in-memory state
        state.setCurrentHighestBid(amount);
        state.setCurrentHighestBidder(bidderId);

        String responsePayload = new Gson().toJson(tx);
        broadcast(new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS", "New bid placed", responsePayload));
        // Trigger auto-bid evaluation for this specific item
        autoBidEngine.triggerEvaluation(targetItemId);
        return new Response(MessageType.BID_SUCCESS, "SUCCESS", "Bid placed successfully!", responsePayload);
    }

    /**
     * Callback for the auto-bid robot to place a bid on a specific item.
     *
     * <p>The {@code itemId} parameter ensures the engine can route bids correctly when
     * multiple auctions run concurrently. Previously this method inferred the item from
     * the global {@code currentAuctionItemId} — meaning only one item could ever receive
     * auto-bids at a time.
     *
     * @param bidderId the ID of the auto-bidder
     * @param nextBid the bid amount to place
     * @param itemId the item to bid on
     * @return {@code true} if the bid was accepted and persisted
     */
    public synchronized boolean processAutoBid(String bidderId, double nextBid, String itemId) {
        AuctionState state = activeAuctions.get(itemId);
        if (state == null) return false;
        if ("FINISHED".equals(state.getStatus())) return false;

        // 1. Kiểm tra số dư ví (Cumulative Stake check)
        double previousStake = bidDAO.getMaxBidAmount(bidderId, itemId);
        double deductionNeeded = nextBid - previousStake;
        double userBalance = walletDAO.getBalance(bidderId);

        if (userBalance < deductionNeeded) {
            System.err.println("[AUTO-BID] Insufficient funds for " + bidderId
                    + " to bid $" + nextBid + " on item " + itemId);
            return false;
        }

        boolean dbUpdated = itemDAO.updateCurrentPrice(itemId, nextBid, bidderId);
        if (dbUpdated) {
            // 2. TRỪ TIỀN (Chỉ trừ phần chênh lệch)
            walletDAO.updateBalance(bidderId, -deductionNeeded);
            System.out.println("[AUTO-BID] Cumulative Stake: Deducted additional $" + deductionNeeded
                    + " from " + bidderId + " (Total: $" + nextBid + ")");

            // 3. Update in-memory state
            state.setCurrentHighestBid(nextBid);
            state.setCurrentHighestBidder(bidderId);
            System.out.println("[AUTO-BID] Auto-bid placed: $" + nextBid
                    + " by " + bidderId + " for item " + itemId);

            // 4. Log transaction
            String txId = "AUTO-" + java.util.UUID.randomUUID().toString().substring(0, 8);
            BidTransaction autoBidTx = new BidTransaction(txId, itemId, bidderId, nextBid, System.currentTimeMillis());
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

    /**
     * Ends the auction for the specified item.
     *
     * <p>Replaces the old no-arg {@code endAuction()} which operated on
     * {@code currentAuctionItemId} — a single global field. With multi-item auctions,
     * each item has its own lifecycle and can end independently.
     *
     * @param itemId the item whose auction to close
     */
    public synchronized void endAuction(String itemId) {
        AuctionState state = activeAuctions.get(itemId);
        if (state == null) return;
        if ("FINISHED".equals(state.getStatus())) return;

        // 1. Persist FINISHED status to DB
        boolean success = itemDAO.updateStatus(itemId, "FINISHED");

        if (success) {
            state.setStatus("FINISHED");
            System.out.println(" [MANAGER] AUCTION ENDED for item " + itemId + "!");

            // 2. Tắt hệ thống Robot cho item này
            // AutoBidEngine will skip FINISHED items on next trigger

            // 3. Hoàn tiền cho tất cả những người tham gia nhưng không thắng
            String winnerId = state.getCurrentHighestBidder();
            List<String> participants = bidDAO.getBiddersForItem(itemId);
            for (String pId : participants) {
                if (!pId.equals(winnerId)) {
                    double refundAmount = bidDAO.getMaxBidAmount(pId, itemId);
                    if (refundAmount > 0) {
                        walletDAO.updateBalance(pId, refundAmount);
                        System.out.println("[ESCROW] Refunded $" + refundAmount + " to " + pId + " (Non-winner)");
                    }
                }
            }

            // 4. Xác định người thắng và thông báo
            if (winnerId != null) {
                String winnerMsg = "Người chiến thắng: " + winnerId
                        + " ($" + state.getCurrentHighestBid() + ")";
                System.out.println(" [MANAGER] " + winnerMsg);

                Item item = itemDAO.getItemById(itemId);
                String sellerId = (item != null) ? item.getSellerId() : "UNKNOWN_SELLER";
                String invoiceId = "INV-" + java.util.UUID.randomUUID().toString().substring(0, 8);
                String auctionId = "AUC-" + itemId;

                // Initialize a PENDING invoice
                com.auction.shared.models.Invoice invoice = new com.auction.shared.models.Invoice(
                    invoiceId, auctionId, itemId,
                    winnerId, sellerId, state.getCurrentHighestBid(),
                    System.currentTimeMillis(), "PENDING"
                );

                if (invoiceDAO.createInvoice(invoice)) {
                    System.out.println("  [INVOICE] Created new invoice: " + invoiceId + " (Status: PENDING)");
                }
            } else {
                System.out.println(" [MANAGER] Phiên kết thúc mà không có ai đặt giá (item " + itemId + ").");
            }

            // 5. Remove from active map — item is done
            activeAuctions.remove(itemId);
        }
    }

    /**
     * Backward-compat no-arg endAuction for tests written before multi-item support.
     * Ends the first active (non-FINISHED) auction it finds.
     *
     * @deprecated Pass an explicit itemId via {@link #endAuction(String)}.
     */
    @Deprecated
    public void endAuction() {
        activeAuctions.keySet().stream().findFirst().ifPresent(this::endAuction);
    }

    public Response getCurrentStatusResponse() {
        // Return status of first active auction (backward compat for single-auction clients)
        String itemId = activeAuctions.keySet().stream().findFirst().orElse(null);
        if (itemId == null) {
            return new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS", "0", "[]");
        }
        return getItemStatusResponse(itemId);
    }

    /**
     * Returns the status for a specific item by ID.
     */
    public Response getItemStatusResponse(String itemId) {
        AuctionState state = activeAuctions.get(itemId);
        List<com.auction.shared.models.BidTransaction> history = bidDAO.getHistoryByItem(itemId);
        String payload = new Gson().toJson(history);

        if (state != null) {
            // Item is active: return live in-memory end time from DB
            Item item = itemDAO.getItemById(itemId);
            long endTime = (item != null) ? item.getEndTime() : 0;
            return new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS", String.valueOf(endTime), payload);
        }

        // Item not in active map: fetch from DB (already finished or not found)
        Item item = itemDAO.getItemById(itemId);
        if (item == null) {
            return new Response(MessageType.BID_ERROR, "FAIL", "Item not found: " + itemId, null);
        }
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

    // --- CANCELLATION PROCESSING (PENALTY & RE-AUCTION) ---
    public synchronized Response processCancellation(String invoiceId, String userId) {
        com.auction.shared.models.Invoice invoice = invoiceDAO.getInvoiceById(invoiceId);
        if (invoice == null || !invoice.getStatus().equals("PENDING")) {
            return new Response(MessageType.BID_ERROR, "FAIL", "Cannot cancel this invoice!", null);
        }

        // 1. Update Invoice status to CANCELED
        invoiceDAO.updateInvoiceStatus(invoiceId, "CANCELED");

        // 2. PENALTY: Transfer the escrowed funds to the Seller as compensation
        walletDAO.updateBalance(invoice.getSellerId(), invoice.getFinalPrice());
        System.out.println(" [PENALTY] Bidder forfeited deposit. Transferred $" + invoice.getFinalPrice() + " to Seller: " + invoice.getSellerId());

        // 3. RE-AUCTION: Reset the item's price and set status back to OPEN
        if (itemDAO.resetItemForReauction(invoice.getItemId())) {
            System.out.println(" [RE-AUCTION] Item " + invoice.getItemId() + " has been reset and is OPEN for bidding again.");

            // Re-register in active auctions map so auto-bidding resumes immediately
            Item resetItem = itemDAO.getItemById(invoice.getItemId());
            if (resetItem != null) {
                registerItemAsActiveAuction(resetItem);
            }

            broadcast(new Response(MessageType.NEW_BID_BROADCAST, "INFO",
                    "A canceled item has returned to the auction floor!", invoice.getItemId()));
        } else {
            // Fallback if reset fails
            itemDAO.updateStatus(invoice.getItemId(), "CANCELED");
        }

        return new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS",
                "Invoice canceled. Penalty applied and item re-auctioned!", invoiceId);
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
