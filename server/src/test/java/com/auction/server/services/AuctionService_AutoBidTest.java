package com.auction.server.services;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.server.services.AuctionServiceTestFixtures.*;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Response;
import com.auction.shared.models.*;
import com.auction.shared.utils.BidIncrementPolicy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Timeout(value = 5, unit = TimeUnit.SECONDS)
@DisplayName("AuctionService — Auto-Bid Tests")
class AuctionService_AutoBidTest {

    static {
        System.setProperty("testMode", "true");
    }

    private AuctionService service;
    private FakeItemDAO fakeItemDAO;
    private FakeBidTransactionDAO fakeBidDAO;
    private FakeWalletDAO fakeWalletDAO;
    private FakeUserDAO fakeUserDAO;

    private static final String ITEM_ID   = "ITEM-001";
    private static final String BIDDER_A  = "userA";
    private static final String BIDDER_B  = "userB";
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

        injectField("itemDAO", fakeItemDAO);
        injectField("bidDAO", fakeBidDAO);
        injectField("walletDAO", fakeWalletDAO);
        injectField("userDAO", fakeUserDAO);

        Item item = new Electronics(ITEM_ID, "Test Item", "Desc", STARTING,
                System.currentTimeMillis(), System.currentTimeMillis() + 3600000L,
                "BrandX", "12 months", "seller1");
        fakeItemDAO.addItem(item);

        primeActiveAuction(ITEM_ID, STARTING, 0.0, null, "OPEN");
    }

    @Test
    @DisplayName("processAutoBid: places bid, deducts wallet, broadcasts, returns true")
    void should_returnTrue_when_autoBidSuccessfullyPlaced() {
        fakeWalletDAO.balances.put(BIDDER_A, 5000.0);

        boolean result = service.processAutoBid(BIDDER_A, STARTING, ITEM_ID);

        assertTrue(result);
        assertEquals(STARTING, service.getCurrentHighestBid(ITEM_ID), 0.001);
        assertEquals(BIDDER_A, service.getCurrentHighestBidder(ITEM_ID));
        assertEquals(5000.0 - STARTING, fakeWalletDAO.getBalance(BIDDER_A), 0.001);
    }

    @Test
    @DisplayName("processAutoBid: returns false when auction is FINISHED")
    void should_returnFalse_when_autoBidOnFinishedAuction() {
        primeActiveAuction(ITEM_ID, STARTING, 0.0, null, "FINISHED");

        boolean result = service.processAutoBid(BIDDER_A, STARTING, ITEM_ID);

        assertFalse(result);
    }

    @Test
    @DisplayName("processAutoBid: returns false when no active auction item")
    void should_returnFalse_when_autoBidNoActiveItem() {
        getActiveAuctions().remove(ITEM_ID);

        boolean result = service.processAutoBid(BIDDER_A, STARTING, ITEM_ID);

        assertFalse(result);
    }

    @Test
    @DisplayName("processAutoBid: returns false and does not deduct when insufficient balance")
    void should_returnFalse_when_autoBidInsufficientFunds() {
        fakeWalletDAO.balances.put(BIDDER_A, 50.0);

        boolean result = service.processAutoBid(BIDDER_A, STARTING, ITEM_ID);

        assertFalse(result);
        assertEquals(50.0, fakeWalletDAO.getBalance(BIDDER_A), 0.001);
    }

    @Test
    @DisplayName("processAutoBid: returns false when DB update fails")
    void should_returnFalse_when_dbUpdateFails() {
        fakeWalletDAO.balances.put(BIDDER_A, 5000.0);
        fakeItemDAO.updatePriceResult = false;

        boolean result = service.processAutoBid(BIDDER_A, STARTING, ITEM_ID);

        assertFalse(result);
        assertEquals(5000.0, fakeWalletDAO.getBalance(BIDDER_A), 0.001);
    }

    @Test
    @DisplayName("registerAutoBid: accepts valid settings when no bids yet (maxPrice >= startingPrice)")
    void should_returnSuccess_when_autoBidValidFirstRegistration() {
        // Use the policy floor for STARTING=$1,240 → $50 (tier: $1,000–$4,999)
        double bidIncrement = BidIncrementPolicy.calculate(STARTING);
        AutoBidSettings settings = new AutoBidSettings(BIDDER_A, ITEM_ID, 2000.0, bidIncrement, false);

        Response res = service.registerAutoBid(settings);

        assertEquals("SUCCESS", res.getStatus());
        assertEquals(MessageType.SETUP_AUTO_BID, res.getType());
    }

    @Test
    @DisplayName("registerAutoBid: rejects when maxPrice is below required minimum (no bids → startingPrice)")
    void should_returnFail_when_maxPriceBelowStartingPrice() {
        AutoBidSettings settings = new AutoBidSettings(BIDDER_A, ITEM_ID, 1000.0, 20.0, false);

        Response res = service.registerAutoBid(settings);

        assertEquals("FAIL", res.getStatus());
        assertTrue(res.getMessage().contains("Giá tối đa không đủ"));
    }

    @Test
    @DisplayName("registerAutoBid: rejects when maxPrice is below currentHighestBid + minIncrement")
    void should_returnFail_when_maxPriceBelowCurrentBidPlusIncrement() {
        primeActiveAuction(ITEM_ID, STARTING, 1500.0, BIDDER_B, "OPEN");
        // Use the policy floor for $1,500 → $50 (tier: $1,000–$4,999)
        double bidIncrement = BidIncrementPolicy.calculate(1500.0);
        AutoBidSettings settings = new AutoBidSettings(BIDDER_A, ITEM_ID, 1510.0, bidIncrement, false);

        Response res = service.registerAutoBid(settings);

        assertEquals("FAIL", res.getStatus());
    }

    @Test
    @DisplayName("registerAutoBid: rejects when bidIncrement is below system minIncrement")
    void should_returnFail_when_bidIncrementTooLow() {
        AutoBidSettings settings = new AutoBidSettings(BIDDER_A, ITEM_ID, 2000.0, 5.0, false);

        Response res = service.registerAutoBid(settings);

        assertEquals("FAIL", res.getStatus());
        assertTrue(res.getMessage().contains("Bước giá quá thấp"));
    }

    @Test
    @DisplayName("registerAutoBid: returns fail when item is not an active auction")
    void should_returnFail_when_itemNotActiveAuction() {
        getActiveAuctions().remove(ITEM_ID);
        AutoBidSettings settings = new AutoBidSettings(BIDDER_A, ITEM_ID, 2000.0, 20.0, false);

        Response res = service.registerAutoBid(settings);

        assertEquals("FAIL", res.getStatus());
        assertTrue(res.getMessage().contains("No active auction found"));
    }

    static Stream<Arguments> allProductTypes() {
        long now = System.currentTimeMillis();
        long end = now + 3_600_000L;
        return Stream.of(
            Arguments.of(new Electronics("ITEM-ELEC", "Laptop", "Gaming laptop", 1000.0, now, end, "Dell", "12m", "seller1"), "Electronics"),
            Arguments.of(new Vehicle("ITEM-VEH", "Tesla Model 3", "EV sedan", 40000.0, now, end, "Tesla", "Model 3", "Red", "seller1"), "Vehicle"),
            Arguments.of(new Sports("ITEM-SPORT", "Tennis Racket", "Pro racket", 150.0, now, end, "Tennis", "White", "seller1"), "Sports"),
            Arguments.of(new Fashion("ITEM-FASH", "Rolex", "Luxury watch", 8000.0, now, end, "Rolex", "M", "Gold", "Steel", "seller1"), "Fashion"),
            Arguments.of(new Collectibles("ITEM-COLL", "Rare Coin", "1900 gold coin", 2500.0, now, end, "Coin", "Rare", "Excellent", "seller1"), "Collectibles")
        );
    }

    @ParameterizedTest(name = "processAutoBid works for: {1}")
    @MethodSource("allProductTypes")
    @DisplayName("processAutoBid: succeeds for any product type without type check")
    void should_processAutoBid_for_anyProductType(Item item, String productLabel) {
        String itemId = item.getId();
        double startPrice = item.getStartingPrice();

        AuctionState state = new AuctionState(itemId, startPrice, startPrice, null);
        getActiveAuctions().put(itemId, state);

        fakeItemDAO.addItem(item);
        fakeWalletDAO.balances.put(BIDDER_A, 999_999.0);

        boolean result = service.processAutoBid(BIDDER_A, startPrice, itemId);

        assertTrue(result, "processAutoBid should succeed for " + productLabel);
        assertEquals(startPrice, service.getCurrentHighestBid(itemId), 0.001);
    }

    @Test
    @DisplayName("registerAutoBid: per-item minIncrement is applied correctly for high-value items")
    void should_usePerItemMinIncrement_when_highValueItem() {
        double vehiclePrice = 40_000.0;
        String vehicleItemId = "ITEM-VEH";

        AuctionState vehicleState = new AuctionState(vehicleItemId, vehiclePrice, vehiclePrice, null);
        getActiveAuctions().put(vehicleItemId, vehicleState);

        double expectedMinInc = AuctionState.computeMinIncrement(vehiclePrice);
        assertEquals(500.0, expectedMinInc, 0.001);

        AutoBidSettings tooLow = new AutoBidSettings(BIDDER_A, vehicleItemId, 50_000.0, 100.0, false);
        Response res = service.registerAutoBid(tooLow);
        assertEquals("FAIL", res.getStatus(), "Increment $100 < $500 should be rejected");

        AutoBidSettings valid = new AutoBidSettings(BIDDER_A, vehicleItemId, 50_000.0, 500.0, false);
        Response res2 = service.registerAutoBid(valid);
        assertEquals("SUCCESS", res2.getStatus(), "Increment $500 should be accepted");
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
}
