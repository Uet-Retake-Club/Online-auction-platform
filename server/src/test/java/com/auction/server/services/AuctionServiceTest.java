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
import com.auction.shared.models.Item;
import com.auction.shared.models.BidTransaction;
import com.google.gson.Gson;

import org.junit.jupiter.api.Timeout;
import java.util.concurrent.TimeUnit;

@Timeout(value = 5, unit = TimeUnit.SECONDS)
public class AuctionServiceTest {

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

        // Mock ItemDAO to bypass database dependencies in tests
        try {
            java.lang.reflect.Field itemDaoField = AuctionService.class.getDeclaredField("itemDAO");
            itemDaoField.setAccessible(true);
            itemDaoField.set(manager, new ItemDAO() {
                @Override
                public Item getItemById(String id) {
                    return null;
                }
                @Override
                public boolean addItem(Item item) {
                    return true;
                }
                @Override
                public boolean updateCurrentPrice(
                    String itemId, double newPrice, String bidderId) {
                    return true;
                }
                @Override
                public boolean updateStatus(String itemId, String status) { return true; }
                @Override
                public com.auction.shared.models.Item getFirstOpenItem() { return null; }
                @Override
                public java.util.List<com.auction.shared.models.Item> getItemsBySellerId(String sellerId) { return java.util.Collections.emptyList(); }
                @Override
                public java.util.List<com.auction.shared.models.Item> getAllItems() { return java.util.Collections.emptyList(); }
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
        AuctionService instance1 = AuctionService.getInstance();
        AuctionService instance2 = AuctionService.getInstance();
        assertSame(instance1, instance2, "getInstance should return the same instance");
    }

    @Test
    void testBroadcast() {
        // Simple test without mock
        manager.broadcast(new Response(MessageType.LOGIN, "SUCCESS", "Test", null));
        // Assume it works if no exception
    }

    @Test
    void testRegisterAutoBidSuccess() {
        AutoBidSettings settings = new AutoBidSettings("client1", "auction1", 1300.0, 50.0, false);
        Response response = manager.registerAutoBid(settings);
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    void testRegisterAutoBidFailLowMaxPrice() {
        AutoBidSettings settings = new AutoBidSettings("client1", "auction1", 1200.0, 50.0, false);
        Response response = manager.registerAutoBid(settings);
        assertEquals("FAIL", response.getStatus());
    }

    @Test
    void testRegisterAutoBidFailLowIncrement() {
        AutoBidSettings settings = new AutoBidSettings("client1", "auction1", 1300.0, 10.0, false);
        Response response = manager.registerAutoBid(settings);
        assertEquals("FAIL", response.getStatus());
    }

    @Test
    void testProcessBidSuccess() {
        Response response = manager.processBid("client1", 1250.0, "payload");
        assertEquals("SUCCESS", response.getStatus());
        // Check currentHighestBid using reflection
        try {
            java.lang.reflect.Field field = AuctionService.class.getDeclaredField("currentHighestBid");
            field.setAccessible(true);
            double currentHighestBid = (double) field.get(manager);
            assertEquals(1250.0, currentHighestBid);
        } catch (Exception e) {
            fail("Reflection failed");
        }
    }

    @Test
    void testProcessBidFail() {
        Response response = manager.processBid("client1", 1200.0, "payload");
        assertEquals("FAIL", response.getStatus());
    }

    @Test
    void testShutdown() {
        manager.shutdown();
        // Hard to test, but assume it works
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
        Response status = manager.getCurrentStatusResponse();
        assertEquals("SUCCESS", status.getStatus());
        assertNotNull(status.getPayload());
        
        BidTransaction bt = new Gson().fromJson(status.getPayload(), BidTransaction.class);
        assertEquals(1240.0, bt.getBidAmount(), 0.001);
        assertEquals("None", bt.getBidderId());
    }

    @Test
    void testGetCurrentStatusResponseAfterBid() {
        manager.processBid("client1", 1300.0, "payload");
        Response status = manager.getCurrentStatusResponse();
        assertEquals("SUCCESS", status.getStatus());
        
        BidTransaction bt = new Gson().fromJson(status.getPayload(), BidTransaction.class);
        assertEquals(1300.0, bt.getBidAmount(), 0.001);
        assertEquals("client1", bt.getBidderId());
    }
}
