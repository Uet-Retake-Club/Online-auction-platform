package com.auction.server.controllers.handlers;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.server.dao.WalletDAO;
import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.shared.models.TopupRequest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link WalletHandler}.
 *
 * <p>Uses manual stubs (anonymous classes) instead of Mockito mocks
 * because Mockito 5.x cannot instrument concrete classes on Java 25.
 *
 * <p>Covers all three sub-commands (GET_WALLET_BALANCE, WALLET_TOPUP_REQUEST,
 * GET_WALLET_HISTORY) plus authentication and invalid-input guards.
 */
@DisplayName("WalletHandler — Unit Tests")
class WalletHandlerTest {

    private static final String USER_ID = "USER-007";

    private WalletHandler handler;

    // ─── Stub helpers ─────────────────────────────────────────────────────────

    /** Minimal ClientHandler stub that returns a fixed clientId without needing a real Socket. */
    static class StubClientHandler extends ClientHandler {
        private final String id;
        StubClientHandler(String id) { super(null); this.id = id; }
        @Override public String getClientId() { return id; }
        @Override public void sendResponse(Response r) { /* no-op */ }
    }

    /** Configurable WalletDAO stub. */
    static class StubWalletDAO implements WalletDAO {
        double balance = 2500.75;
        boolean topupSuccess = true;
        List<TopupRequest> history = Collections.emptyList();

        @Override public double getBalance(String userId)                        { return balance; }
        @Override public boolean updateBalance(String userId, double amount)     { return true; }
        @Override public boolean createTopupRequest(String userId, double amount){ return topupSuccess; }
        @Override public List<TopupRequest> getPendingRequests()                 { return Collections.emptyList(); }
        @Override public List<TopupRequest> getHistory(String userId)            { return history; }
        @Override public boolean updateRequestStatus(String requestId, String s) { return true; }
    }

    private StubWalletDAO stubDAO;

    @BeforeEach
    void setUp() throws Exception {
        handler = new WalletHandler();
        stubDAO  = new StubWalletDAO();

        // Inject stub DAO into the handler's private final field
        Field daoField = WalletHandler.class.getDeclaredField("walletDAO");
        daoField.setAccessible(true);
        daoField.set(handler, stubDAO);
    }

    // ─── Authentication guard ─────────────────────────────────────────────────

    @Test
    @DisplayName("handle: returns FAIL when client is not authenticated (Unknown)")
    void should_returnFail_when_clientNotAuthenticated() {
        Request req = new Request(MessageType.GET_WALLET_BALANCE, "Unknown", "");
        Response res = handler.handle(req, new StubClientHandler("Unknown"));

        assertEquals("FAIL", res.getStatus());
    }

    @Test
    @DisplayName("handle: returns FAIL when clientId is null")
    void should_returnFail_when_clientIdIsNull() {
        Request req = new Request(MessageType.GET_WALLET_BALANCE, null, "");
        Response res = handler.handle(req, new StubClientHandler(null));

        assertEquals("FAIL", res.getStatus());
    }

    // ─── GET_WALLET_BALANCE ───────────────────────────────────────────────────

    @Test
    @DisplayName("GET_WALLET_BALANCE: returns balance as string payload")
    void should_returnBalance_when_getWalletBalanceRequested() {
        stubDAO.balance = 2500.75;

        Request req = new Request(MessageType.GET_WALLET_BALANCE, USER_ID, "");
        Response res = handler.handle(req, new StubClientHandler(USER_ID));

        assertEquals(MessageType.WALLET_BALANCE_RESPONSE, res.getType());
        assertEquals("SUCCESS", res.getStatus());
        assertEquals("2500.75", res.getPayload());
    }

    @Test
    @DisplayName("GET_WALLET_BALANCE: returns 0.0 when user has no wallet record")
    void should_returnZero_when_walletNotFound() {
        stubDAO.balance = 0.0;

        Request req = new Request(MessageType.GET_WALLET_BALANCE, USER_ID, "");
        Response res = handler.handle(req, new StubClientHandler(USER_ID));

        assertEquals("SUCCESS", res.getStatus());
        assertEquals("0.0", res.getPayload());
    }

    // ─── WALLET_TOPUP_REQUEST ─────────────────────────────────────────────────

    @Test
    @DisplayName("WALLET_TOPUP_REQUEST: returns SUCCESS when top-up request created")
    void should_returnSuccess_when_topupRequestCreated() {
        stubDAO.topupSuccess = true;

        Request req = new Request(MessageType.WALLET_TOPUP_REQUEST, USER_ID, "500.0");
        Response res = handler.handle(req, new StubClientHandler(USER_ID));

        assertEquals(MessageType.WALLET_TOPUP_APPROVE, res.getType());
        assertEquals("SUCCESS", res.getStatus());
        assertTrue(res.getMessage().contains("submitted"));
    }

    @Test
    @DisplayName("WALLET_TOPUP_REQUEST: returns FAIL when DAO fails to persist")
    void should_returnFail_when_topupDaoFails() {
        stubDAO.topupSuccess = false;

        Request req = new Request(MessageType.WALLET_TOPUP_REQUEST, USER_ID, "500.0");
        Response res = handler.handle(req, new StubClientHandler(USER_ID));

        assertEquals("FAIL", res.getStatus());
    }

    @ParameterizedTest(name = "invalid amount: \"{0}\"")
    @ValueSource(strings = {"abc", "", "12.34.56", "not-a-number"})
    @DisplayName("WALLET_TOPUP_REQUEST: returns FAIL when payload is not a valid number")
    void should_returnFail_when_topupAmountIsInvalid(String badAmount) {
        Request req = new Request(MessageType.WALLET_TOPUP_REQUEST, USER_ID, badAmount);
        Response res = handler.handle(req, new StubClientHandler(USER_ID));

        assertEquals("FAIL", res.getStatus());
        assertEquals("Invalid amount", res.getMessage());
    }

    // ─── GET_WALLET_HISTORY ───────────────────────────────────────────────────

    @Test
    @DisplayName("GET_WALLET_HISTORY: returns JSON array of topup history")
    void should_returnHistory_when_requested() {
        stubDAO.history = List.of(
            new TopupRequest("TR-001", USER_ID, 500.0, "PENDING", System.currentTimeMillis())
        );

        Request req = new Request(MessageType.GET_WALLET_HISTORY, USER_ID, "");
        Response res = handler.handle(req, new StubClientHandler(USER_ID));

        assertEquals(MessageType.WALLET_HISTORY_RESPONSE, res.getType());
        assertEquals("SUCCESS", res.getStatus());
        assertTrue(res.getPayload().contains("TR-001"));
    }

    @Test
    @DisplayName("GET_WALLET_HISTORY: returns empty JSON array when no history exists")
    void should_returnEmptyArray_when_noHistoryExists() {
        stubDAO.history = Collections.emptyList();

        Request req = new Request(MessageType.GET_WALLET_HISTORY, USER_ID, "");
        Response res = handler.handle(req, new StubClientHandler(USER_ID));

        assertEquals("SUCCESS", res.getStatus());
        assertEquals("[]", res.getPayload());
    }

    // ─── Unknown command ──────────────────────────────────────────────────────

    @Test
    @DisplayName("handle: returns FAIL for unrecognized MessageType")
    void should_returnFail_when_unknownWalletCommand() {
        Request req = new Request(MessageType.LOGIN, USER_ID, "");
        Response res = handler.handle(req, new StubClientHandler(USER_ID));

        assertEquals("FAIL", res.getStatus());
        assertTrue(res.getMessage().contains("Unknown wallet command"));
    }
}
