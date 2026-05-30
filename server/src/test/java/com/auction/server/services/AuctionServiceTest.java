package com.auction.server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.auction.server.services.AuctionServiceTestFixtures.*;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Response;
import com.auction.shared.models.AuctionState;
import com.auction.shared.models.AutoBidSettings;
import com.auction.shared.models.BidTransaction;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Timeout(value = 5, unit = TimeUnit.SECONDS)
public class AuctionServiceTest {

    static {
        System.setProperty("testMode", "true");
    }

    private static final String TEST_ITEM = "TEST-ITEM-001";
    private static final double STARTING  = 1240.0;

    private AuctionService manager;
    private FakeItemDAO fakeItemDAO;
    private FakeBidTransactionDAO fakeBidDAO;
    private FakeWalletDAO fakeWalletDAO;
    private FakeUserDAO fakeUserDAO;
    private FakeAuctionDAO fakeAuctionDAO;

    @BeforeEach
    void setUp() throws Exception {
        Field instanceField = AuctionService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        AuctionService existing = (AuctionService) instanceField.get(null);
        if (existing != null) existing.shutdown();
        instanceField.set(null, null);

        manager = AuctionService.getInstance();

        fakeItemDAO = new FakeItemDAO();
        fakeBidDAO = new FakeBidTransactionDAO();
        fakeWalletDAO = new FakeWalletDAO();
        fakeUserDAO = new FakeUserDAO();
        fakeAuctionDAO = new FakeAuctionDAO();

        injectField("itemDAO", fakeItemDAO);
        injectField("bidDAO", fakeBidDAO);
        injectField("walletDAO", fakeWalletDAO);
        injectField("userDAO", fakeUserDAO);
        injectField("auctionDAO", fakeAuctionDAO);

        fakeWalletDAO.balances.put("client1", 1_000_000.0);
        fakeWalletDAO.balances.put("client2", 1_000_000.0);

        primeActiveAuction(TEST_ITEM, STARTING, 0.0, null, "OPEN");
    }

    private void injectField(String name, Object val) throws Exception {
        Field f = AuctionService.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(manager, val);
    }

    @SuppressWarnings("unchecked")
    private Map<String, AuctionState> getActiveAuctions() throws Exception {
        Field f = AuctionService.class.getDeclaredField("activeAuctions");
        f.setAccessible(true);
        return (Map<String, AuctionState>) f.get(manager);
    }

    private void primeActiveAuction(
            String itemId, double startingPrice, double currentBid,
            String currentBidder, String status) {
        try {
            AuctionState state = new AuctionState(itemId, startingPrice, currentBid, currentBidder);
            state.setStatus(status);
            getActiveAuctions().put(itemId, state);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String payload(String itemId, double amount) {
        return String.format(java.util.Locale.US, "{\"itemId\":\"%s\",\"bidAmount\":%.2f}", itemId, amount);
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
        Response res = manager.processBid("client1", 1250.0, payload(TEST_ITEM, 1250.0));
        assertEquals("SUCCESS", res.getStatus());
        assertEquals(1250.0, manager.getCurrentHighestBid(TEST_ITEM));
        assertEquals("client1", manager.getCurrentHighestBidder(TEST_ITEM));
    }

    @Test
    void testProcessBidFail() {
        Response res = manager.processBid("client1", 1000.0, payload(TEST_ITEM, 1000.0));
        assertEquals("FAIL", res.getStatus());
    }

    @Test
    void testProcessBidUpdatesHighestBidder() {
        manager.processBid("client1", 1250.0, payload(TEST_ITEM, 1250.0));
        assertEquals("client1", manager.getCurrentHighestBidder(TEST_ITEM));
    }

    @Test
    void testProcessSecondBidRequiresIncrement() {
        // First bid at starting price succeeds
        Response first = manager.processBid("client1", 1240.0, payload(TEST_ITEM, 1240.0));
        assertEquals("SUCCESS", first.getStatus());

        // Floor at $1,240 = $50 → min next = $1,290
        // $1,250 is only +$10, below the $50 floor — must FAIL
        Response tooLow = manager.processBid("client2", 1250.0, payload(TEST_ITEM, 1250.0));
        assertEquals("FAIL", tooLow.getStatus());

        // $1,290 = $1,240 + $50 — exactly at the minimum — must SUCCESS
        Response enough = manager.processBid("client2", 1290.0, payload(TEST_ITEM, 1290.0));
        assertEquals("SUCCESS", enough.getStatus());
    }

    @Test
    void testGetCurrentStatusResponseBeforeAnyBid() {
        Response status = manager.getCurrentStatusResponse();
        assertEquals("SUCCESS", status.getStatus());
        assertNotNull(status.getPayload());

        BidTransaction[] history = new Gson().fromJson(status.getPayload(), BidTransaction[].class);
        assertEquals(0, history.length);
    }

    @Test
    void testGetCurrentStatusResponseAfterBid() {
        manager.processBid("client1", 1300.0, payload(TEST_ITEM, 1300.0));
        Response status = manager.getCurrentStatusResponse();
        assertEquals("SUCCESS", status.getStatus());

        BidTransaction[] history = new Gson().fromJson(status.getPayload(), BidTransaction[].class);
        assertEquals(1, history.length);
        assertEquals(1300.0, history[0].getBidAmount(), 0.001);
        assertEquals("client1", history[0].getBidderId());
    }

    @Test
    void testRegisterAutoBidSuccess() {
        // STARTING=$1,240 → floor=$50 (tier $1,000–$4,999)
        double validIncrement = com.auction.shared.utils.BidIncrementPolicy.calculate(STARTING);
        AutoBidSettings settings = new AutoBidSettings("client1", TEST_ITEM, 2000.0, validIncrement, false);
        Response res = manager.registerAutoBid(settings);
        assertEquals("SUCCESS", res.getStatus());
    }

    @Test
    void testRegisterAutoBidFailLowMaxPrice() {
        AutoBidSettings settings = new AutoBidSettings("client1", TEST_ITEM, 1000.0, 20.0, false);
        Response res = manager.registerAutoBid(settings);
        assertEquals("FAIL", res.getStatus());
    }

    @Test
    void testRegisterAutoBidFailLowIncrement() {
        AutoBidSettings settings = new AutoBidSettings("client1", TEST_ITEM, 2000.0, 5.0, false);
        Response res = manager.registerAutoBid(settings);
        assertEquals("FAIL", res.getStatus());
    }

    @Test
    void testBroadcast() {
        manager.broadcast(new Response(MessageType.NEW_BID_BROADCAST, "SUCCESS", "Test", null));
    }

    @Test
    void testShutdown() {
        manager.shutdown();
        manager.shutdown();
    }
}