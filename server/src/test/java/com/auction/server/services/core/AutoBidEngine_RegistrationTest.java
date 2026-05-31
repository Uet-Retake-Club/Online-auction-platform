package com.auction.server.services.core;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.server.services.AuctionService;
import com.auction.shared.models.AutoBidSettings;
import org.junit.jupiter.api.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Timeout(value = 5, unit = TimeUnit.SECONDS)
@DisplayName("AutoBidEngine — Registration & Lifecycle Tests")
class AutoBidEngine_RegistrationTest {

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
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    @DisplayName("triggerEvaluation: no task submitted when no auto-bidders registered")
    void should_notSubmitTask_when_noBiddersRegistered() throws Exception {
        AuctionService stub = AutoBidEngineTestFixtures.buildStub(
            AutoBidEngineTestFixtures.BIDDER_B, AutoBidEngineTestFixtures.CURRENT_BID, true, null);
        engine = new AutoBidEngine(stub);

        engine.triggerEvaluation(AutoBidEngineTestFixtures.ITEM_A);
        engine.shutdown();
        engine = null;
    }

    @Test
    @DisplayName("addAutoBidder: registers bidder without exception")
    void should_registerBidder_without_exception() throws Exception {
        AuctionService stub = AutoBidEngineTestFixtures.buildStub(
            AutoBidEngineTestFixtures.BIDDER_B, AutoBidEngineTestFixtures.CURRENT_BID, true, null);
        engine = new AutoBidEngine(stub);

        assertDoesNotThrow(() ->
            engine.addAutoBidder(new AutoBidSettings(
                AutoBidEngineTestFixtures.BIDDER_A, AutoBidEngineTestFixtures.ITEM_A, 2000.0, 50.0, false)));
    }

    @Test
    @Timeout(value = 4, unit = TimeUnit.SECONDS)
    @DisplayName("removeAutoBidder: removed bidder does not participate in evaluation")
    void should_notParticipate_when_bidderRemoved() throws Exception {
        AtomicInteger bidCalls = new AtomicInteger(0);
        AuctionService stub = AutoBidEngineTestFixtures.buildStub(
            AutoBidEngineTestFixtures.BIDDER_B, AutoBidEngineTestFixtures.CURRENT_BID, true, bidCalls);
        engine = new AutoBidEngine(stub);

        engine.addAutoBidder(new AutoBidSettings(
            AutoBidEngineTestFixtures.BIDDER_A, AutoBidEngineTestFixtures.ITEM_A, 2000.0, 50.0, false));
        engine.removeAutoBidder(AutoBidEngineTestFixtures.BIDDER_A);

        engine.triggerEvaluation(AutoBidEngineTestFixtures.ITEM_A);
        engine.shutdown();
        engine = null;

        assertEquals(0, bidCalls.get(), "Removed bidder should not participate");
    }

    @Test
    @DisplayName("shutdown: completes without exception when no bidders registered")
    void should_shutdownSafely_when_noBiddersRegistered() throws Exception {
        AuctionService stub = AutoBidEngineTestFixtures.buildStub(
            AutoBidEngineTestFixtures.BIDDER_B, AutoBidEngineTestFixtures.CURRENT_BID, true, null);
        engine = new AutoBidEngine(stub);
        assertDoesNotThrow(() -> engine.shutdown());
        engine = null;
    }

    @Test
    @DisplayName("shutdown: idempotent — second call does not throw")
    void should_beIdempotent_when_shutdownCalledTwice() throws Exception {
        AuctionService stub = AutoBidEngineTestFixtures.buildStub(
            AutoBidEngineTestFixtures.BIDDER_B, AutoBidEngineTestFixtures.CURRENT_BID, true, null);
        engine = new AutoBidEngine(stub);
        assertDoesNotThrow(() -> {
            engine.shutdown();
            engine.shutdown();
        });
        engine = null;
    }
}
