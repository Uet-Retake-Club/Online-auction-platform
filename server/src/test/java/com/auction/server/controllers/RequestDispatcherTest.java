package com.auction.server.controllers;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.server.exceptions.AuctionException;
import com.auction.server.exceptions.DatabaseOperationException;
import com.auction.server.exceptions.InvalidBidException;
import com.auction.server.network.ClientHandler;
import com.auction.server.services.AuctionService;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;

import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Unit tests for {@link RequestDispatcher}.
 *
 * <p>Uses manual stubs (anonymous classes) instead of Mockito mocks
 * because Mockito 5.x cannot instrument concrete classes on Java 25.
 *
 * <p>Covers:
 * <ul>
 *   <li>Known command delegates to registered handler → handler response returned</li>
 *   <li>Unknown command → ERROR "Lệnh không được hệ thống hỗ trợ!"</li>
 *   <li>Handler throws {@link InvalidBidException} → FAIL response</li>
 *   <li>Handler throws {@link DatabaseOperationException} → ERROR response</li>
 *   <li>Handler throws {@link AuctionException} → FAIL response</li>
 *   <li>Handler throws generic {@link RuntimeException} → ERROR response</li>
 *   <li>GET_STATUS with unauthenticated client → no crash</li>
 * </ul>
 */
@DisplayName("RequestDispatcher — Unit Tests")
class RequestDispatcherTest {

    static {
        System.setProperty("testMode", "true");
    }

    private RequestDispatcher dispatcher;

    /** Minimal ClientHandler stub that returns "Unknown" as the client ID. */
    static class StubClientHandler extends ClientHandler {
        private final String id;
        StubClientHandler(String id) { super(null); this.id = id; }
        @Override public String getClientId() { return id; }
        @Override public void sendResponse(Response r) { /* no-op */ }
    }

    private final StubClientHandler unknownClient = new StubClientHandler("Unknown");

    @BeforeEach
    void setUp() throws Exception {
        // Reset AuctionService singleton to avoid real network/DB threads
        Field instanceField = AuctionService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        AuctionService existing = (AuctionService) instanceField.get(null);
        if (existing != null) existing.shutdown();
        instanceField.set(null, null);

        dispatcher = new RequestDispatcher();
    }

    // ─── Unknown command ──────────────────────────────────────────────────────

    @Test
    @DisplayName("dispatch: returns ERROR for unregistered MessageType")
    void should_returnError_when_handlerNotFound() {
        // AUCTION_ENDED is never registered as a handler
        Request req = new Request(MessageType.AUCTION_ENDED, "client1", "{}");
        Response res = dispatcher.dispatch(req, unknownClient);

        assertEquals("ERROR", res.getStatus());
        assertTrue(res.getMessage().contains("không được hệ thống hỗ trợ"),
            "Expected 'không được hệ thống hỗ trợ' but got: " + res.getMessage());
    }

    // ─── Happy path ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("dispatch: delegates to registered handler and returns its response")
    void should_returnHandlerResponse_when_handlerRegistered() throws Exception {
        Response expectedResponse = new Response(MessageType.BID_SUCCESS, "SUCCESS", "OK", null);
        CommandHandler stubHandler = (req, ch) -> expectedResponse;
        injectHandler(MessageType.PLACE_BID, stubHandler);

        Request req = new Request(MessageType.PLACE_BID, "client1", "{}");
        Response res = dispatcher.dispatch(req, unknownClient);

        assertSame(expectedResponse, res);
    }

    // ─── Exception routing ────────────────────────────────────────────────────

    @Test
    @DisplayName("dispatch: returns FAIL when handler throws InvalidBidException")
    void should_returnFail_when_handlerThrowsInvalidBidException() throws Exception {
        injectHandler(MessageType.PLACE_BID,
            (req, ch) -> { throw new InvalidBidException("Bid too low"); });

        Request req = new Request(MessageType.PLACE_BID, "client1", "{}");
        Response res = dispatcher.dispatch(req, unknownClient);

        assertEquals("FAIL", res.getStatus());
        assertEquals("Bid too low", res.getMessage());
        assertEquals(MessageType.PLACE_BID, res.getType());
    }

    @Test
    @DisplayName("dispatch: returns ERROR when handler throws DatabaseOperationException")
    void should_returnError_when_handlerThrowsDatabaseOperationException() throws Exception {
        injectHandler(MessageType.PLACE_BID,
            (req, ch) -> { throw new DatabaseOperationException("DB lost", null); });

        Request req = new Request(MessageType.PLACE_BID, "client1", "{}");
        Response res = dispatcher.dispatch(req, unknownClient);

        assertEquals("ERROR", res.getStatus());
        assertTrue(res.getMessage().contains("lưu trữ") || res.getMessage().contains("bận"),
            "Expected DB busy/storage message, got: " + res.getMessage());
    }

    @Test
    @DisplayName("dispatch: returns FAIL when handler throws AuctionException")
    void should_returnFail_when_handlerThrowsAuctionException() throws Exception {
        injectHandler(MessageType.PLACE_BID,
            (req, ch) -> { throw new AuctionException("Auction rule violated"); });

        Request req = new Request(MessageType.PLACE_BID, "client1", "{}");
        Response res = dispatcher.dispatch(req, unknownClient);

        assertEquals("FAIL", res.getStatus());
        assertEquals("Auction rule violated", res.getMessage());
    }

    @Test
    @DisplayName("dispatch: returns ERROR when handler throws unexpected RuntimeException")
    void should_returnError_when_handlerThrowsRuntimeException() throws Exception {
        injectHandler(MessageType.PLACE_BID,
            (req, ch) -> { throw new NullPointerException("Unexpected null"); });

        Request req = new Request(MessageType.PLACE_BID, "client1", "{}");
        Response res = dispatcher.dispatch(req, unknownClient);

        assertEquals("ERROR", res.getStatus());
        assertTrue(res.getMessage().contains("Lỗi máy chủ") || res.getMessage().contains("nội bộ"),
            "Expected internal server error message, got: " + res.getMessage());
    }

    @Test
    @DisplayName("dispatch: GET_STATUS with unauthenticated client does not crash")
    void should_notCrash_when_getStatusCalledWithUnknownClient() {
        Request req = new Request(MessageType.GET_STATUS, "Unknown", "");
        assertDoesNotThrow(() -> dispatcher.dispatch(req, unknownClient));
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void injectHandler(MessageType type, CommandHandler handler) throws Exception {
        Field registryField = RequestDispatcher.class.getDeclaredField("handlerRegistry");
        registryField.setAccessible(true);
        Map<MessageType, CommandHandler> registry =
                (Map<MessageType, CommandHandler>) registryField.get(dispatcher);
        registry.put(type, handler);
    }
}
