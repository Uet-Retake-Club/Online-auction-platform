package com.auction.server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Response;
import com.auction.shared.models.AutoBidSettings;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.BidTransactionDAO;
import com.auction.server.dao.WalletDAO;
import com.auction.shared.models.Item;
import com.auction.shared.models.BidTransaction;
import com.google.gson.Gson;

import org.junit.jupiter.api.Timeout;
import java.util.concurrent.TimeUnit;

@Timeout(value = 5, unit = TimeUnit.SECONDS)
public class AuctionServiceTest {

    static {
        System.setProperty("testMode", "true");
    }

    private AuctionService manager;

    @BeforeEach
    void setUp() {
        // Explicitly shut down existing instance if present to stop its threads
        try {
            java.lang.reflect.Field instanceField = AuctionService.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            AuctionService existing = (AuctionService) instanceField.get(null);
            if (existing != null) {
                existing.shutdown();
            }
            instanceField.set(null, null); // Reset for new instance
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        manager = AuctionService.getInstance();

        // Mock DAOs to bypass database dependencies in tests
        try {
            java.lang.reflect.Field itemDaoField = AuctionService.class.getDeclaredField("itemDAO");
            itemDaoField.setAccessible(true);
            itemDaoField.set(manager, new ItemDAO() {
                @Override public Item getItemById(String id) { return null; }
                @Override public boolean addItem(Item item) { return true; }
                @Override public boolean updateCurrentPrice(String itemId, double newPrice, String bidderId) { return true; }
                @Override public boolean updateStatus(String itemId, String status) { return true; }
                @Override public Item getFirstOpenItem() { return null; }
                @Override public java.util.List<Item> getItemsBySellerId(String sellerId) { return java.util.Collections.emptyList(); }
                @Override public java.util.List<Item> getAllItems() { return java.util.Collections.emptyList(); }
                @Override public int getActiveAuctionCount() { return 0; }
            });

            java.lang.reflect.Field bidDaoField = AuctionService.class.getDeclaredField("bidDAO");
            bidDaoField.setAccessible(true);
            bidDaoField.set(manager, new BidTransactionDAO() {
                private final java.util.List<BidTransaction> mockBids = new java.util.ArrayList<>();

                @Override
                public boolean addTransaction(BidTransaction tx) {
                    mockBids.add(tx);
                    return true;
                }

                @Override
                public java.util.List<BidTransaction> getHistoryByItem(String itemId) {
                    return mockBids;
                }

                @Override
                public java.util.List<BidTransaction> getAllTransactions() {
                    return mockBids;
                }

                @Override
                public int getTotalBidCount() {
                    return mockBids.size();
                }

                @Override
                public double getMaxBidAmount(String userId, String itemId) {
                    double max = 0.0;
                    for (BidTransaction tx : mockBids) {
                        if (tx.getBidderId().equals(userId) && tx.getItemId().equals(itemId)) {
                            if (tx.getBidAmount() > max) max = tx.getBidAmount();
                        }
                    }
                    return max;
                }

                @Override
                public java.util.List<String> getBiddersForItem(String itemId) {
                    java.util.List<String> bidders = new java.util.ArrayList<>();
                    for (BidTransaction tx : mockBids) {
                        if (tx.getItemId().equals(itemId) && !bidders.contains(tx.getBidderId())) {
                            bidders.add(tx.getBidderId());
                        }
                    }
                    return bidders;
                }
            });

            java.lang.reflect.Field walletDaoField = AuctionService.class.getDeclaredField("walletDAO");
            walletDaoField.setAccessible(true);
            walletDaoField.set(manager, new WalletDAO() {
                @Override public double getBalance(String userId) { return 1000000.0; } // Rich for tests
                @Override public boolean updateBalance(String userId, double amount) { return true; }
                @Override public boolean createTopupRequest(String userId, double amount) { return true; }
                @Override public java.util.List<com.auction.shared.models.TopupRequest> getPendingRequests() { return java.util.Collections.emptyList(); }
                @Override public java.util.List<com.auction.shared.models.TopupRequest> getHistory(String userId) { return java.util.Collections.emptyList(); }
                @Override public boolean updateRequestStatus(String requestId, String status) { return true; }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set currentAuctionItemId and startingPrice so bid logic isn't blocked
        try {
            java.lang.reflect.Field itemIdField = AuctionService.class.getDeclaredField("currentAuctionItemId");
            itemIdField.setAccessible(true);
            itemIdField.set(manager, "TEST-ITEM-001");

            java.lang.reflect.Field priceField = AuctionService.class.getDeclaredField("startingPrice");
            priceField.setAccessible(true);
            priceField.set(manager, 1240.0);

            // Ensure auctionStatus is OPEN for tests
            java.lang.reflect.Field statusField = AuctionService.class.getDeclaredField("auctionStatus");
            statusField.setAccessible(true);
            statusField.set(manager, "OPEN");

            // Reset highest bid state to prevent leakage from real DB
            java.lang.reflect.Field bidField = AuctionService.class.getDeclaredField("currentHighestBid");
            bidField.setAccessible(true);
            bidField.set(manager, 0.0);

            java.lang.reflect.Field bidderField = AuctionService.class.getDeclaredField("currentHighestBidder");
            bidderField.setAccessible(true);
            bidderField.set(manager, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testGetInstance() {
        AuctionService s1 = AuctionService.getInstance();
        AuctionService s2 = AuctionService.getInstance();
        assertSame(s1, s2);
        assertNotNull(s1);
    }

    @Test
    void testProcessBidSuccess() {
        Response res = manager.processBid("client1", 1250.0, "payload");
        assertEquals("SUCCESS", res.getStatus());
        assertEquals(1250.0, manager.getCurrentHighestBid());
        assertEquals("client1", manager.getCurrentHighestBidder());
    }

    @Test
    void testProcessBidFail() {
        // AuctionStatus is OPEN, but price is too low
        Response res = manager.processBid("client1", 1000.0, "payload");
        assertEquals("FAIL", res.getStatus());
    }

    @Test
    void testProcessBidUpdatesHighestBidder() {
        manager.processBid("client1", 1250.0, "payload");
        try {
            java.lang.reflect.Field field = AuctionService.class.getDeclaredField("currentHighestBidder");
            field.setAccessible(true);
            String highestBidder = (String) field.get(manager);
            assertEquals("client1", highestBidder);
        } catch (Exception e) {
            fail("Reflection failed");
        }
    }

    @Test
    void testProcessSecondBidRequiresIncrement() {
        // Người đầu tiên đặt giá thành công
        Response first = manager.processBid("client1", 1240.0, "payload");
        assertEquals("SUCCESS", first.getStatus());

        // Người thứ 2 phải >= 1240 + 20 = 1260
        Response tooLow = manager.processBid("client2", 1250.0, "payload");
        assertEquals("FAIL", tooLow.getStatus());

        // Người thứ 2 đặt đúng mức tối thiểu
        Response enough = manager.processBid("client2", 1260.0, "payload");
        assertEquals("SUCCESS", enough.getStatus());
    }

    @Test
    void testGetCurrentStatusResponseBeforeAnyBid() {
        // Initially no bids, empty history returned
        Response status = manager.getCurrentStatusResponse();
        assertEquals("SUCCESS", status.getStatus());
        assertNotNull(status.getPayload());
        
        BidTransaction[] history = new Gson().fromJson(status.getPayload(), BidTransaction[].class);
        assertEquals(0, history.length);
    }

    @Test
    void testGetCurrentStatusResponseAfterBid() {
        manager.processBid("client1", 1300.0, "payload");
        Response status = manager.getCurrentStatusResponse();
        assertEquals("SUCCESS", status.getStatus());
        
        BidTransaction[] history = new Gson().fromJson(status.getPayload(), BidTransaction[].class);
        assertEquals(1, history.length);
        assertEquals(1300.0, history[0].getBidAmount(), 0.001);
        assertEquals("client1", history[0].getBidderId());
    }

    @Test
    void testRegisterAutoBidSuccess() {
        AutoBidSettings settings = new AutoBidSettings("client1", "TEST-ITEM-001", 2000.0, 20.0, false);
        Response res = manager.registerAutoBid(settings);
        assertEquals("SUCCESS", res.getStatus());
    }

    @Test
    void testRegisterAutoBidFailLowMaxPrice() {
        // Current highest bid is 0, but starting price is 1240. Max price 1000 is too low.
        AutoBidSettings settings = new AutoBidSettings("client1", "TEST-ITEM-001", 1000.0, 20.0, false);
        Response res = manager.registerAutoBid(settings);
        assertEquals("FAIL", res.getStatus());
    }

    @Test
    void testRegisterAutoBidFailLowIncrement() {
        AutoBidSettings settings = new AutoBidSettings("client1", "TEST-ITEM-001", 2000.0, 5.0, false);
        Response res = manager.registerAutoBid(settings);
        assertEquals("FAIL", res.getStatus());
    }

    @Test
    void testBroadcast() {
        // Simply ensure it doesn't crash without connected clients
        manager.broadcast(new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS", "Test", null));
    }

    @Test
    void testShutdown() {
        manager.shutdown();
        // Subsequent calls should be safe
        manager.shutdown();
    }

}
