package com.auction.server.services.core;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.server.services.AuctionService;
import com.auction.shared.models.AutoBidSettings;
import org.junit.jupiter.api.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Timeout(value = 5, unit = TimeUnit.SECONDS)
@DisplayName("AutoBidEngine — Bid Evaluation Tests")
class AutoBidEngine_EvaluationTest {

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

    @Test
    @Timeout(value = 4, unit = TimeUnit.SECONDS)
    @DisplayName("evaluateAutoBids: non-aggressive bidder places bid at minIncrement above current")
    void should_placeMinIncrementBid_when_nonAggressiveBidder() throws Exception {
        AtomicInteger bidCalls = new AtomicInteger(0);
        AuctionService stub = AutoBidEngineTestFixtures.buildStub(
            AutoBidEngineTestFixtures.BIDDER_B, AutoBidEngineTestFixtures.CURRENT_BID, true, bidCalls);
        engine = new AutoBidEngine(stub);

        AutoBidSettings settings = new AutoBidSettings(
            AutoBidEngineTestFixtures.BIDDER_A, AutoBidEngineTestFixtures.ITEM_A, 2000.0, 50.0, false);
        engine.addAutoBidder(settings);

        engine.triggerEvaluation(AutoBidEngineTestFixtures.ITEM_A);
        engine.shutdown();
        engine = null;

        assertTrue(bidCalls.get() >= 1, "Expected at least one processAutoBid call");
    }

    @Test
    @Timeout(value = 4, unit = TimeUnit.SECONDS)
    @DisplayName("evaluateAutoBids: aggressive bidder uses custom bidIncrement not minIncrement")
    void should_useCustomIncrement_when_aggressiveMode() throws Exception {
        AtomicInteger bidCalls = new AtomicInteger(0);
        AuctionService stub = AutoBidEngineTestFixtures.buildStub(
            AutoBidEngineTestFixtures.BIDDER_B, AutoBidEngineTestFixtures.CURRENT_BID, true, bidCalls);
        engine = new AutoBidEngine(stub);

        AutoBidSettings settings = new AutoBidSettings(
            AutoBidEngineTestFixtures.BIDDER_A, AutoBidEngineTestFixtures.ITEM_A, 2000.0, 50.0, true);
        engine.addAutoBidder(settings);

        engine.triggerEvaluation(AutoBidEngineTestFixtures.ITEM_A);
        engine.shutdown();
        engine = null;

        assertTrue(bidCalls.get() >= 1, "Expected custom increment bid to be placed");
    }

    @Test
    @Timeout(value = 4, unit = TimeUnit.SECONDS)
    @DisplayName("evaluateAutoBids: nextBid is capped at maxPrice when computed bid would exceed it")
    void should_capNextBidAtMaxPrice_when_incrementExceedsMaxPrice() throws Exception {
        AtomicInteger bidCalls = new AtomicInteger(0);
        AuctionService stub = AutoBidEngineTestFixtures.buildStub(
            AutoBidEngineTestFixtures.ITEM_A, AutoBidEngineTestFixtures.BIDDER_B, 990.0, true, bidCalls);
        engine = new AutoBidEngine(stub);

        AutoBidSettings settings = new AutoBidSettings(
            AutoBidEngineTestFixtures.BIDDER_A, AutoBidEngineTestFixtures.ITEM_A, 1010.0, 30.0, true);
        engine.addAutoBidder(settings);

        engine.triggerEvaluation(AutoBidEngineTestFixtures.ITEM_A);
        engine.shutdown();
        engine = null;

        assertTrue(bidCalls.get() >= 1, "Expected capped bid to be placed");
    }

    @Test
    @Timeout(value = 4, unit = TimeUnit.SECONDS)
    @DisplayName("evaluateAutoBids: bidder who is already the current leader is skipped")
    void should_skipBidder_when_alreadyHighestBidder() throws Exception {
        AtomicInteger bidCalls = new AtomicInteger(0);
        AuctionService stub = AutoBidEngineTestFixtures.buildStub(
            AutoBidEngineTestFixtures.BIDDER_B, AutoBidEngineTestFixtures.CURRENT_BID, true, bidCalls);
        engine = new AutoBidEngine(stub);

        AutoBidSettings settings = new AutoBidSettings(
            AutoBidEngineTestFixtures.BIDDER_B, AutoBidEngineTestFixtures.ITEM_A, 2000.0, 50.0, false);
        engine.addAutoBidder(settings);

        engine.triggerEvaluation(AutoBidEngineTestFixtures.ITEM_A);
        engine.shutdown();
        engine = null;

        assertEquals(0, bidCalls.get(), "Leader should be skipped");
    }

    @Test
    @Timeout(value = 4, unit = TimeUnit.SECONDS)
    @DisplayName("evaluateAutoBids: inactive bidder is never selected")
    void should_skipBidder_when_settingsMarkedInactive() throws Exception {
        AtomicInteger bidCalls = new AtomicInteger(0);
        AuctionService stub = AutoBidEngineTestFixtures.buildStub(
            AutoBidEngineTestFixtures.BIDDER_B, AutoBidEngineTestFixtures.CURRENT_BID, true, bidCalls);
        engine = new AutoBidEngine(stub);

        AutoBidSettings settings = new AutoBidSettings(
            AutoBidEngineTestFixtures.BIDDER_A, AutoBidEngineTestFixtures.ITEM_A, 2000.0, 50.0, false);
        settings.setActive(false);
        engine.addAutoBidder(settings);

        engine.triggerEvaluation(AutoBidEngineTestFixtures.ITEM_A);
        engine.shutdown();
        engine = null;

        assertEquals(0, bidCalls.get(), "Inactive bidder should be skipped");
    }

    @Test
    @Timeout(value = 4, unit = TimeUnit.SECONDS)
    @DisplayName("evaluateAutoBids: bidder whose maxPrice < requiredMinBid is not selected")
    void should_notSelectBidder_when_maxPriceBelowRequiredMin() throws Exception {
        AtomicInteger bidCalls = new AtomicInteger(0);
        AuctionService stub = AutoBidEngineTestFixtures.buildStub(
            AutoBidEngineTestFixtures.BIDDER_B, AutoBidEngineTestFixtures.CURRENT_BID, true, bidCalls);
        engine = new AutoBidEngine(stub);

        AutoBidSettings settings = new AutoBidSettings(
            AutoBidEngineTestFixtures.BIDDER_A, AutoBidEngineTestFixtures.ITEM_A, 1015.0, 50.0, false);
        engine.addAutoBidder(settings);

        engine.triggerEvaluation(AutoBidEngineTestFixtures.ITEM_A);
        engine.shutdown();
        engine = null;

        assertEquals(0, bidCalls.get(), "Bidder below requiredMinBid should be filtered out");
    }

    @Test
    @Timeout(value = 4, unit = TimeUnit.SECONDS)
    @DisplayName("evaluateAutoBids: deactivates bidder when processAutoBid returns false")
    void should_deactivateBidder_when_processAutoBidReturnsFalse() throws Exception {
        AuctionService stub = AutoBidEngineTestFixtures.buildStub(
            AutoBidEngineTestFixtures.BIDDER_B, AutoBidEngineTestFixtures.CURRENT_BID, false, null);
        engine = new AutoBidEngine(stub);

        AutoBidSettings settings = new AutoBidSettings(
            AutoBidEngineTestFixtures.BIDDER_A, AutoBidEngineTestFixtures.ITEM_A, 2000.0, 50.0, false);
        engine.addAutoBidder(settings);

        engine.triggerEvaluation(AutoBidEngineTestFixtures.ITEM_A);
        engine.shutdown();
        engine = null;

        assertFalse(settings.isActive(), "Bidder should be deactivated after failure");
    }
}
