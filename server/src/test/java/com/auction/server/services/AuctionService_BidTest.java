package com.auction.server.services;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.server.services.AuctionServiceTestFixtures.*;
import com.auction.shared.dto.Response;
import com.auction.shared.models.AuctionState;
import com.auction.shared.models.BidTransaction;
import com.auction.shared.models.Electronics;
import com.auction.shared.models.Item;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Timeout(value = 5, unit = TimeUnit.SECONDS)
@DisplayName("AuctionService — Bid Processing Tests")
class AuctionService_BidTest {

    static {
        System.setProperty("testMode", "true");
    }

    private AuctionService service;
    private FakeItemDAO fakeItemDAO;
    private FakeBidTransactionDAO fakeBidDAO;
    private FakeWalletDAO fakeWalletDAO;
    private FakeUserDAO fakeUserDAO;
    private FakeAuctionDAO fakeAuctionDAO;

    private static final String ITEM_ID   = "ITEM-001";
    private static final String BIDDER_A  = "userA";
    private static final String SELLER_ID = "seller1";
    private static final double STARTING  = 1240.0;

    @BeforeEach
    void setUp() throws Exception {
        Field instanceField = AuctionService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        AuctionService existing = (AuctionService) instanceField.get(null);
        if (existing != null) existing.shutdown();
        instanceField.set(null, null);

        service = AuctionService.getInstance();

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

        // Put active item in DB fake
        Item item = buildItem(ITEM_ID, SELLER_ID);
        fakeItemDAO.addItem(item);

        primeActiveAuction(ITEM_ID, STARTING, 0.0, null, "OPEN");
    }

    @Test
    @DisplayName("processBid: first bid at starting price deducts full amount from wallet")
    void should_deductFullAmount_when_firstBidAtStartingPrice() {
        fakeWalletDAO.balances.put(BIDDER_A, 5000.0);

        Response res = service.processBid(BIDDER_A, STARTING, buildPayload(ITEM_ID, STARTING));

        assertEquals("SUCCESS", res.getStatus(), "Expected SUCCESS: " + res.getMessage());
        assertEquals(5000.0 - STARTING, fakeWalletDAO.getBalance(BIDDER_A), 0.001);
        assertEquals(-STARTING, fakeWalletDAO.balanceUpdates.get(BIDDER_A).get(0), 0.001);
    }

    @Test
    @DisplayName("processBid: second bid by same user only deducts the incremental difference")
    void should_deductOnlyDifference_when_sameUserIncreaseBid() {
        fakeWalletDAO.balances.put(BIDDER_A, 5000.0);
        fakeBidDAO.addTransaction(new BidTransaction("tx1", ITEM_ID, BIDDER_A, STARTING, System.currentTimeMillis()));

        primeActiveAuction(ITEM_ID, STARTING, STARTING, BIDDER_A, "OPEN");

        // Floor at $1,240 = $50 → minimum next bid = $1,290
        // Bidder A increases from $1,240 → $1,290; deduction = $1,290 - $1,240 = $50
        Response res = service.processBid(BIDDER_A, 1290.0, buildPayload(ITEM_ID, 1290.0));

        assertEquals("SUCCESS", res.getStatus());
        assertEquals(5000.0 - 50.0, fakeWalletDAO.getBalance(BIDDER_A), 0.001);
        assertEquals(-50.0, fakeWalletDAO.balanceUpdates.get(BIDDER_A).get(0), 0.001);
    }

    @Test
    @DisplayName("processBid: rejects bid when wallet balance is insufficient")
    void should_returnFail_when_walletInsufficientFunds() {
        fakeWalletDAO.balances.put(BIDDER_A, 100.0);

        Response res = service.processBid(BIDDER_A, STARTING, buildPayload(ITEM_ID, STARTING));

        assertEquals("FAIL", res.getStatus());
        assertTrue(res.getMessage().contains("Số dư ví không đủ"));
        assertTrue(fakeItemDAO.updatedPrices.isEmpty());
    }

    @Test
    @DisplayName("processBid: rejects all bids when auction status is FINISHED")
    void should_returnFail_when_auctionAlreadyFinished() {
        primeActiveAuction(ITEM_ID, STARTING, 0.0, null, "FINISHED");
        fakeWalletDAO.balances.put(BIDDER_A, 99999.0);

        Response res = service.processBid(BIDDER_A, STARTING, buildPayload(ITEM_ID, STARTING));

        assertEquals("FAIL", res.getStatus());
        assertTrue(res.getMessage().contains("ended"));
        assertTrue(fakeItemDAO.updatedPrices.isEmpty());
    }

    @Test
    @DisplayName("processBid: rejects bid when no active auction exists for item")
    void should_returnFail_when_noActiveAuction() {
        getActiveAuctions().remove(ITEM_ID);

        Response res = service.processBid(BIDDER_A, STARTING, buildPayload(ITEM_ID, STARTING));

        assertEquals("FAIL", res.getStatus());
    }

    @ParameterizedTest(name = "bid amount={0} is below minimum")
    @ValueSource(doubles = {0.0, 500.0, 1239.99})
    @DisplayName("processBid: rejects bids below starting price (no previous bids)")
    void should_returnFail_when_bidBelowStartingPrice(double amount) {
        fakeWalletDAO.balances.put(BIDDER_A, 99999.0);

        Response res = service.processBid(BIDDER_A, amount, buildPayload(ITEM_ID, amount));

        assertEquals("FAIL", res.getStatus());
    }

    private void injectField(String fieldName, Object mock) throws Exception {
        Field f = AuctionService.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(service, mock);
    }

    @SuppressWarnings("unchecked")
    private Map<String, AuctionState> getActiveAuctions() {
        try {
            Field f = AuctionService.class.getDeclaredField("activeAuctions");
            f.setAccessible(true);
            return (Map<String, AuctionState>) f.get(service);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get activeAuctions", e);
        }
    }

    private void primeActiveAuction(
            String itemId, double startingPrice,
            double currentBid, String currentBidder, String status) {
        AuctionState state = new AuctionState(itemId, startingPrice, currentBid, currentBidder);
        state.setStatus(status);
        getActiveAuctions().put(itemId, state);
    }

    private String buildPayload(String itemId, double amount) {
        return String.format(java.util.Locale.US, "{\"itemId\":\"%s\",\"bidAmount\":%.2f}", itemId, amount);
    }

    private Item buildItem(String id, String sellerId) {
        Electronics e = new Electronics(id, "Test Item", "Desc", 1000.0,
                System.currentTimeMillis(), System.currentTimeMillis() + 3600000L,
                "BrandX", "12 months", sellerId);
        e.setStatus("OPEN");
        return e;
    }
}
