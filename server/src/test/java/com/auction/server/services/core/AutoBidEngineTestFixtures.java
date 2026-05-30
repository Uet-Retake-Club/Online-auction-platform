package com.auction.server.services.core;

import com.auction.server.network.ClientHandler;
import com.auction.server.services.AuctionService;
import com.auction.shared.dto.Response;
import com.auction.shared.models.AuctionState;
import com.auction.shared.models.AutoBidSettings;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

class AutoBidEngineTestFixtures {
    static final double CURRENT_BID = 1000.0;
    static final double MIN_INC     = 20.0;
    static final String ITEM_A      = "ITEM-001";
    static final String ITEM_B      = "ITEM-002";
    static final String BIDDER_A    = "userA";
    static final String BIDDER_B    = "userB";

    static AuctionService buildStub(
            String itemId,
            String currentBidder,
            double currentBid,
            boolean autoBidResult,
            AtomicInteger callCounter) throws Exception {

        Field instanceField = AuctionService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        AuctionService existing = (AuctionService) instanceField.get(null);
        if (existing != null) existing.shutdown();
        instanceField.set(null, null);

        AuctionService stub = AuctionService.getInstance();

        AuctionState state = new AuctionState(itemId, currentBid, currentBid, currentBidder);
        Map<String, AuctionState> activeAuctions = new ConcurrentHashMap<>();
        activeAuctions.put(itemId, state);
        setField(stub, "activeAuctions", activeAuctions);

        injectNullDaos(stub, autoBidResult, callCounter);

        return stub;
    }

    static AuctionService buildStub(
            String currentBidder,
            double currentBid,
            boolean autoBidResult,
            AtomicInteger callCounter) throws Exception {
        return buildStub(ITEM_A, currentBidder, currentBid, autoBidResult, callCounter);
    }

    private static void injectNullDaos(AuctionService stub, boolean bidResult, AtomicInteger counter)
            throws Exception {

        com.auction.server.dao.WalletDAO walletStub = new com.auction.server.dao.WalletDAO() {
            @Override public double getBalance(String u) { return 9_999_999.0; }
            @Override public boolean updateBalance(String u, double a) { return true; }
            @Override public boolean createTopupRequest(String u, double a) { return true; }
            @Override public java.util.List<com.auction.shared.models.TopupRequest>
                    getPendingRequests() { return java.util.Collections.emptyList(); }
            @Override public java.util.List<com.auction.shared.models.TopupRequest>
                    getHistory(String u) { return java.util.Collections.emptyList(); }
            @Override public boolean updateRequestStatus(String id, String s) { return true; }
        };

        com.auction.server.dao.ItemDAO itemStub = new com.auction.server.dao.ItemDAO() {
            @Override public boolean updateCurrentPrice(String id, double p, String b) {
                if (counter != null) counter.incrementAndGet();
                return bidResult;
            }
            @Override public com.auction.shared.models.Item getItemById(String id) { return null; }
            @Override public boolean addItem(com.auction.shared.models.Item i) { return true; }
            @Override public boolean updateStatus(String id, String s) { return true; }
            @Override public com.auction.shared.models.Item getFirstOpenItem() { return null; }
            @Override public java.util.List<com.auction.shared.models.Item>
                    getItemsBySellerId(String s) { return java.util.Collections.emptyList(); }
            @Override public java.util.List<com.auction.shared.models.Item>
                    getAllItems() { return java.util.Collections.emptyList(); }
            @Override public int getActiveAuctionCount() { return 0; }
            @Override public boolean resetItemForReauction(String id) { return true; }
            
            // --- BỔ SUNG 2 HÀM MỚI CHO ANTI-SNIPING ---
            @Override public boolean updateEndTime(String itemId, long newEndTime) { return true; }
            @Override public boolean compareAndSetEndTime(String itemId, long expectedEndTime, long newEndTime) { return true; }
        };

        com.auction.server.dao.BidTransactionDAO bidStub = new com.auction.server.dao.BidTransactionDAO() {
            @Override public boolean addTransaction(com.auction.shared.models.BidTransaction t) { return true; }
            @Override public java.util.List<com.auction.shared.models.BidTransaction>
                    getHistoryByItem(String i) { return java.util.Collections.emptyList(); }
            @Override public java.util.List<com.auction.shared.models.BidTransaction>
                    getAllTransactions() { return java.util.Collections.emptyList(); }
            @Override public int getTotalBidCount() { return 0; }
            @Override public double getMaxBidAmount(String u, String i) { return 0.0; }
            @Override public java.util.List<String> getBiddersForItem(String i) { return java.util.Collections.emptyList(); }
            @Override public java.util.List<String> getBiddedItemIds(String u) { return java.util.Collections.emptyList(); }
        };

        com.auction.server.dao.UserDAO userStub = new com.auction.server.dao.UserDAO() {
            @Override public com.auction.shared.models.User getUserById(String id) { return null; }
            @Override public com.auction.shared.models.User getUserByUsername(String u) { return null; }
            @Override public boolean addUser(com.auction.shared.models.User u, String p) { return true; }
            @Override public String authenticateUser(String u, String p) { return null; }
            @Override public java.util.List<com.auction.shared.models.User> getAllUsers() { return java.util.Collections.emptyList(); }
            @Override public int getUserCount() { return 0; }
            @Override public boolean updateUserStatus(String id, String s) { return true; }
        };

        setField(stub, "walletDAO", walletStub);
        setField(stub, "itemDAO",   itemStub);
        setField(stub, "bidDAO",    bidStub);
        setField(stub, "userDAO",   userStub);
    }

    static void setField(Object obj, String name, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }
    
}