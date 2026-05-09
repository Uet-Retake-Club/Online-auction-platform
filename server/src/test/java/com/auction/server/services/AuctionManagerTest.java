package com.auction.server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Response;
import com.auction.shared.models.AutoBidSettings;

public class AuctionManagerTest {

    private AuctionManager manager;

    @BeforeEach
    void setUp() {
        // Reset singleton instance for each test
        try {
            java.lang.reflect.Field instance = AuctionManager.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        manager = AuctionManager.getInstance();
    }

    @Test
    void testGetInstance() {
        AuctionManager instance1 = AuctionManager.getInstance();
        AuctionManager instance2 = AuctionManager.getInstance();
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
            java.lang.reflect.Field field = AuctionManager.class.getDeclaredField("currentHighestBid");
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
            java.lang.reflect.Field field = AuctionManager.class.getDeclaredField("currentHighestBidder");
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
        // Payload chứa giá khởi điểm 1240.0 vì chưa ai đặt
        assertTrue(status.getPayload().contains("1240.0"));
    }

    @Test
    void testGetCurrentStatusResponseAfterBid() {
        manager.processBid("client1", 1300.0, "payload");
        Response status = manager.getCurrentStatusResponse();
        assertEquals("SUCCESS", status.getStatus());
        assertTrue(status.getPayload().contains("1300.0"));
        assertTrue(status.getPayload().contains("client1"));
    }
}
