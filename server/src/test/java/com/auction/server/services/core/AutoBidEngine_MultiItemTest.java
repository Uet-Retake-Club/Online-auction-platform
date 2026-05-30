package com.auction.server.services.core;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.auction.server.services.AuctionService;
import com.auction.shared.models.AuctionState;
import com.auction.shared.models.AutoBidSettings;

@Timeout(value = 5, unit = TimeUnit.SECONDS)
@DisplayName("AutoBidEngine — Multi-Item & Product Agnostic Tests")
class AutoBidEngine_MultiItemTest {

    static {
        System.setProperty("testMode", "true");
    }

    private AutoBidEngine engine;

    @AfterEach
    void tearDown() {
        if (engine != null) {
            engine.shutdown();
        }
    }

    static Stream<Arguments> productTypeProvider() {
        return Stream.of(
            Arguments.of("ITEM-ELECTRONICS", "Electronics"),
            Arguments.of("ITEM-VEHICLE",     "Vehicle"),
            Arguments.of("ITEM-SPORTS",      "Sports"),
            Arguments.of("ITEM-FASHION",     "Fashion"),
            Arguments.of("ITEM-COLLECTIBLES","Collectibles"),
            Arguments.of("ITEM-HOME",        "HomeAndGarden"),
            Arguments.of("ITEM-OTHER",       "OtherItem")
        );
    }

    @ParameterizedTest(name = "auto-bid works for product type: {1}")
    @MethodSource("productTypeProvider")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("evaluateAutoBids: engine is product-type-agnostic across all categories")
    void should_processAutoBid_regardless_of_productType(String itemId, String productLabel)
            throws Exception {
        AtomicInteger bidCalls = new AtomicInteger(0);
        AuctionService stub = AutoBidEngineTestFixtures.buildStub(
            itemId, AutoBidEngineTestFixtures.BIDDER_B, AutoBidEngineTestFixtures.CURRENT_BID, true, bidCalls);
        engine = new AutoBidEngine(stub);

        AutoBidSettings settings = new AutoBidSettings(
            AutoBidEngineTestFixtures.BIDDER_A, itemId, 2000.0, 50.0, false);
        engine.addAutoBidder(settings);

        engine.triggerEvaluation(itemId);
        engine.shutdown();
        engine = null;

        assertTrue(bidCalls.get() >= 1, "Expected auto-bid call for " + productLabel);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("multi-item isolation: auto-bid for item A does not trigger bid on item B")
    void should_notCrossContaminate_when_differentItemIds() throws Exception {
        AtomicInteger bidCalls = new AtomicInteger(0);

        Field instanceField = AuctionService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        AuctionService existing = (AuctionService) instanceField.get(null);
        if (existing != null) existing.shutdown();
        instanceField.set(null, null);

        AuctionService stub = AuctionService.getInstance();

        AuctionState stateA = new AuctionState(
            AutoBidEngineTestFixtures.ITEM_A, AutoBidEngineTestFixtures.CURRENT_BID, AutoBidEngineTestFixtures.CURRENT_BID, AutoBidEngineTestFixtures.BIDDER_B);
        AuctionState stateB = new AuctionState(
            AutoBidEngineTestFixtures.ITEM_B, AutoBidEngineTestFixtures.CURRENT_BID, AutoBidEngineTestFixtures.CURRENT_BID, AutoBidEngineTestFixtures.BIDDER_B);
        Map<String, AuctionState> activeAuctions = new ConcurrentHashMap<>();
        activeAuctions.put(AutoBidEngineTestFixtures.ITEM_A, stateA);
        activeAuctions.put(AutoBidEngineTestFixtures.ITEM_B, stateB);
        AutoBidEngineTestFixtures.setField(stub, "activeAuctions", activeAuctions);

        // Replace DAOs in stub using reflection directly (similar to injectNullDaos)
        Field walletDaoField = AuctionService.class.getDeclaredField("walletDAO");
        walletDaoField.setAccessible(true);
        walletDaoField.set(stub, new com.auction.server.dao.WalletDAO() {
            @Override public double getBalance(String u) { return 9999999.0; }
            @Override public boolean updateBalance(String u, double a) { return true; }
            @Override public boolean createTopupRequest(String u, double a) { return true; }
            @Override public java.util.List<com.auction.shared.models.TopupRequest> getPendingRequests() { return java.util.Collections.emptyList(); }
            @Override public java.util.List<com.auction.shared.models.TopupRequest> getHistory(String u) { return java.util.Collections.emptyList(); }
            @Override public boolean updateRequestStatus(String id, String s) { return true; }
        });

        Field itemDaoField = AuctionService.class.getDeclaredField("itemDAO");
        itemDaoField.setAccessible(true);
        itemDaoField.set(stub, new com.auction.server.dao.ItemDAO() {
            @Override public boolean updateCurrentPrice(String id, double p, String b) {
                bidCalls.incrementAndGet();
                return true;
            }
            @Override public com.auction.shared.models.Item getItemById(String id) { return null; }
            @Override public boolean addItem(com.auction.shared.models.Item i) { return true; }
            @Override public boolean updateStatus(String id, String s) { return true; }
            @Override public com.auction.shared.models.Item getFirstOpenItem() { return null; }
            @Override public java.util.List<com.auction.shared.models.Item> getItemsBySellerId(String s) { return java.util.Collections.emptyList(); }
            @Override public java.util.List<com.auction.shared.models.Item> getAllItems() { return java.util.Collections.emptyList(); }
            @Override public int getActiveAuctionCount() { return 0; }
            @Override public boolean resetItemForReauction(String id) { return true; }
            
            // --- ANTI-SNIPING METHODS ADDED ---
            @Override public boolean updateEndTime(String itemId, long newEndTime) { return true; }
            @Override public boolean compareAndSetEndTime(String itemId, long expectedEndTime, long newEndTime) { return true; }
        });

        Field bidDaoField = AuctionService.class.getDeclaredField("bidDAO");
        bidDaoField.setAccessible(true);
        bidDaoField.set(stub, new com.auction.server.dao.BidTransactionDAO() {
            @Override public boolean addTransaction(com.auction.shared.models.BidTransaction t) { return true; }
            @Override public java.util.List<com.auction.shared.models.BidTransaction> getHistoryByItem(String i) { return java.util.Collections.emptyList(); }
            @Override public java.util.List<com.auction.shared.models.BidTransaction> getAllTransactions() { return java.util.Collections.emptyList(); }
            @Override public int getTotalBidCount() { return 0; }
            @Override public double getMaxBidAmount(String u, String i) { return 0.0; }
            @Override public java.util.List<String> getBiddersForItem(String i) { return java.util.Collections.emptyList(); }
            @Override public java.util.List<String> getBiddedItemIds(String u) { return java.util.Collections.emptyList(); }
        });

        engine = new AutoBidEngine(stub);

        AutoBidSettings settings = new AutoBidSettings(
            AutoBidEngineTestFixtures.BIDDER_A, AutoBidEngineTestFixtures.ITEM_A, 2000.0, 50.0, false);
        engine.addAutoBidder(settings);

        engine.triggerEvaluation(AutoBidEngineTestFixtures.ITEM_B);
        engine.shutdown();
        engine = null;

        assertEquals(0, bidCalls.get(), "Auto-bidder for item A must not bid on item B");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("system stability: adding a new product type does not affect existing bidders")
    void should_notAffectExistingBidders_when_newProductTypeAdded() throws Exception {
        AtomicInteger bidCallsA = new AtomicInteger(0);

        Field instanceField = AuctionService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        AuctionService existing = (AuctionService) instanceField.get(null);
        if (existing != null) existing.shutdown();
        instanceField.set(null, null);

        AuctionService stub = AuctionService.getInstance();

        AuctionState stateA = new AuctionState(
            AutoBidEngineTestFixtures.ITEM_A, AutoBidEngineTestFixtures.CURRENT_BID, AutoBidEngineTestFixtures.CURRENT_BID, AutoBidEngineTestFixtures.BIDDER_B);
        Map<String, AuctionState> activeAuctions = new ConcurrentHashMap<>();
        activeAuctions.put(AutoBidEngineTestFixtures.ITEM_A, stateA);
        AutoBidEngineTestFixtures.setField(stub, "activeAuctions", activeAuctions);

        // Replace DAOs in stub using reflection directly
        Field walletDaoField = AuctionService.class.getDeclaredField("walletDAO");
        walletDaoField.setAccessible(true);
        walletDaoField.set(stub, new com.auction.server.dao.WalletDAO() {
            @Override public double getBalance(String u) { return 9999999.0; }
            @Override public boolean updateBalance(String u, double a) { return true; }
            @Override public boolean createTopupRequest(String u, double a) { return true; }
            @Override public java.util.List<com.auction.shared.models.TopupRequest> getPendingRequests() { return java.util.Collections.emptyList(); }
            @Override public java.util.List<com.auction.shared.models.TopupRequest> getHistory(String u) { return java.util.Collections.emptyList(); }
            @Override public boolean updateRequestStatus(String id, String s) { return true; }
        });

        Field itemDaoField = AuctionService.class.getDeclaredField("itemDAO");
        itemDaoField.setAccessible(true);
        itemDaoField.set(stub, new com.auction.server.dao.ItemDAO() {
            @Override public boolean updateCurrentPrice(String id, double p, String b) {
                bidCallsA.incrementAndGet();
                return true;
            }
            @Override public com.auction.shared.models.Item getItemById(String id) { return null; }
            @Override public boolean addItem(com.auction.shared.models.Item i) { return true; }
            @Override public boolean updateStatus(String id, String s) { return true; }
            @Override public com.auction.shared.models.Item getFirstOpenItem() { return null; }
            @Override public java.util.List<com.auction.shared.models.Item> getItemsBySellerId(String s) { return java.util.Collections.emptyList(); }
            @Override public java.util.List<com.auction.shared.models.Item> getAllItems() { return java.util.Collections.emptyList(); }
            @Override public int getActiveAuctionCount() { return 0; }
            @Override public boolean resetItemForReauction(String id) { return true; }
            
            // --- ANTI-SNIPING METHODS ADDED ---
            @Override public boolean updateEndTime(String itemId, long newEndTime) { return true; }
            @Override public boolean compareAndSetEndTime(String itemId, long expectedEndTime, long newEndTime) { return true; }
        });

        Field bidDaoField = AuctionService.class.getDeclaredField("bidDAO");
        bidDaoField.setAccessible(true);
        bidDaoField.set(stub, new com.auction.server.dao.BidTransactionDAO() {
            @Override public boolean addTransaction(com.auction.shared.models.BidTransaction t) { return true; }
            @Override public java.util.List<com.auction.shared.models.BidTransaction> getHistoryByItem(String i) { return java.util.Collections.emptyList(); }
            @Override public java.util.List<com.auction.shared.models.BidTransaction> getAllTransactions() { return java.util.Collections.emptyList(); }
            @Override public int getTotalBidCount() { return 0; }
            @Override public double getMaxBidAmount(String u, String i) { return 0.0; }
            @Override public java.util.List<String> getBiddersForItem(String i) { return java.util.Collections.emptyList(); }
            @Override public java.util.List<String> getBiddedItemIds(String u) { return java.util.Collections.emptyList(); }
        });

        engine = new AutoBidEngine(stub);

        AutoBidSettings settingsA = new AutoBidSettings(
            AutoBidEngineTestFixtures.BIDDER_A, AutoBidEngineTestFixtures.ITEM_A, 2000.0, 50.0, false);
        engine.addAutoBidder(settingsA);

        // Add a NEW product type's item at runtime
        AuctionState stateB = new AuctionState(
            AutoBidEngineTestFixtures.ITEM_B, 5000.0, 5000.0, null);
        activeAuctions.put(AutoBidEngineTestFixtures.ITEM_B, stateB);

        engine.triggerEvaluation(AutoBidEngineTestFixtures.ITEM_A);
        engine.shutdown();
        engine = null;

        assertTrue(bidCallsA.get() >= 1, "ITEM_A auto-bid should still fire");
    }
}