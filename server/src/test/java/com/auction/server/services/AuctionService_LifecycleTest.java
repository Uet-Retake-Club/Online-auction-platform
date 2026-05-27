package com.auction.server.services;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.server.services.AuctionServiceTestFixtures.*;
import com.auction.shared.models.*;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Timeout(value = 5, unit = TimeUnit.SECONDS)
@DisplayName("AuctionService — Lifecycle & Auction Ends Tests")
class AuctionService_LifecycleTest {

    static {
        System.setProperty("testMode", "true");
    }

    private AuctionService service;
    private FakeItemDAO fakeItemDAO;
    private FakeBidTransactionDAO fakeBidDAO;
    private FakeWalletDAO fakeWalletDAO;
    private FakeUserDAO fakeUserDAO;
    private FakeInvoiceDAO fakeInvoiceDAO;
    private FakeAuctionDAO fakeAuctionDAO;

    private static final String ITEM_ID   = "ITEM-001";
    private static final String BIDDER_A  = "userA";
    private static final String BIDDER_B  = "userB";
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
        fakeInvoiceDAO = new FakeInvoiceDAO();
        fakeAuctionDAO = new FakeAuctionDAO();

        injectField("itemDAO", fakeItemDAO);
        injectField("bidDAO", fakeBidDAO);
        injectField("walletDAO", fakeWalletDAO);
        injectField("invoiceDAO", fakeInvoiceDAO);
        injectField("userDAO", fakeUserDAO);
        injectField("auctionDAO", fakeAuctionDAO);

        Item item = buildItem(ITEM_ID, SELLER_ID);
        fakeItemDAO.addItem(item);

        User seller = new Seller(SELLER_ID, "seller", "seller@x.com", "ACTIVE");
        fakeUserDAO.addUser(seller, "pass");

        primeActiveAuction(ITEM_ID, STARTING, 0.0, null, "OPEN");
    }

    @Test
    @DisplayName("endAuction: sets status FINISHED and creates invoice when there is a winner")
    void should_finishAuctionAndCreateInvoice_when_thereIsAWinner() {
        primeActiveAuction(ITEM_ID, STARTING, 1500.0, BIDDER_A, "OPEN");
        fakeBidDAO.itemBidders.computeIfAbsent(ITEM_ID, k -> new java.util.ArrayList<>()).add(BIDDER_A);

        service.endAuction(ITEM_ID);

        assertEquals("FINISHED", fakeItemDAO.updatedStatuses.get(ITEM_ID));
        Invoice inv = fakeInvoiceDAO.invoices.values().stream().findFirst().orElse(null);
        assertNotNull(inv);
        assertEquals("PENDING", inv.getStatus());
        assertEquals(1500.0, inv.getFinalPrice());
        assertEquals(BIDDER_A, inv.getBidderId());
    }

    @Test
    @DisplayName("endAuction: refunds non-winning bidders their staked amount")
    void should_refundNonWinners_when_auctionEnds() {
        primeActiveAuction(ITEM_ID, STARTING, 1500.0, BIDDER_A, "OPEN");
        fakeBidDAO.itemBidders.computeIfAbsent(ITEM_ID, k -> new java.util.ArrayList<>()).add(BIDDER_A);
        fakeBidDAO.itemBidders.get(ITEM_ID).add(BIDDER_B);

        fakeBidDAO.addTransaction(new BidTransaction("t1", ITEM_ID, BIDDER_A, 1500.0, 100));
        fakeBidDAO.addTransaction(new BidTransaction("t2", ITEM_ID, BIDDER_B, 1300.0, 90));

        fakeWalletDAO.balances.put(BIDDER_B, 0.0);

        service.endAuction(ITEM_ID);

        assertEquals(1300.0, fakeWalletDAO.getBalance(BIDDER_B), 0.001);
        assertNull(fakeWalletDAO.balanceUpdates.get(BIDDER_A));
    }

    @Test
    @DisplayName("endAuction: does nothing when auction is already FINISHED (idempotent guard)")
    void should_beIdempotent_when_endAuctionCalledTwice() {
        primeActiveAuction(ITEM_ID, STARTING, 0.0, null, "FINISHED");

        service.endAuction(ITEM_ID);

        assertTrue(fakeItemDAO.updatedStatuses.isEmpty());
    }

    @Test
    @DisplayName("endAuction: does nothing when item not in active auctions")
    void should_doNothing_when_noCurrentAuctionItem() {
        getActiveAuctions().remove(ITEM_ID);

        service.endAuction(ITEM_ID);

        assertTrue(fakeItemDAO.updatedStatuses.isEmpty());
    }

    @Test
    @DisplayName("endAuction: skips invoice creation when no one placed a bid")
    void should_notCreateInvoice_when_noBidsPlaced() {
        primeActiveAuction(ITEM_ID, STARTING, 0.0, null, "OPEN");

        service.endAuction(ITEM_ID);

        assertTrue(fakeInvoiceDAO.invoices.isEmpty());
    }

    @Test
    @DisplayName("AuctionState.computeMinIncrement: returns $5 for low-value items (<= $100)")
    void should_return5_when_startingPriceBelow100() {
        assertEquals(5.0, AuctionState.computeMinIncrement(50.0), 0.001);
        assertEquals(5.0, AuctionState.computeMinIncrement(100.0), 0.001);
    }

    @Test
    @DisplayName("AuctionState.computeMinIncrement: returns $20 for mid-value items ($101-$2000)")
    void should_return20_when_startingPriceMidRange() {
        assertEquals(20.0, AuctionState.computeMinIncrement(101.0), 0.001);
        assertEquals(20.0, AuctionState.computeMinIncrement(2000.0), 0.001);
        assertEquals(20.0, AuctionState.computeMinIncrement(1240.0), 0.001);
    }

    @Test
    @DisplayName("AuctionState.computeMinIncrement: returns $100 for high-value items ($2001-$20000)")
    void should_return100_when_startingPriceHighRange() {
        assertEquals(100.0, AuctionState.computeMinIncrement(2001.0), 0.001);
        assertEquals(100.0, AuctionState.computeMinIncrement(20_000.0), 0.001);
    }

    @Test
    @DisplayName("AuctionState.computeMinIncrement: returns $500 for very high-value items (> $20000)")
    void should_return500_when_startingPricePremium() {
        assertEquals(500.0, AuctionState.computeMinIncrement(20_001.0), 0.001);
        assertEquals(500.0, AuctionState.computeMinIncrement(40_000.0), 0.001);
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

    private Item buildItem(String id, String sellerId) {
        Electronics e = new Electronics(id, "Test Item", "Desc", 1000.0,
                System.currentTimeMillis(), System.currentTimeMillis() + 3600000L,
                "BrandX", "12 months", sellerId);
        e.setStatus("OPEN");
        return e;
    }
}
