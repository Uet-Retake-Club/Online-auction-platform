package com.auction.server.controllers.handlers;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.server.dao.UserDAO;
import com.auction.server.network.ClientHandler;
import com.auction.server.services.AuctionService;
import com.auction.shared.dto.AuthPayload;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.shared.models.Bidder;
import com.auction.shared.models.User;
import com.google.gson.Gson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

@DisplayName("LoginHandler — Unit Tests")
class LoginHandlerTest {

    static {
        System.setProperty("testMode", "true");
    }

    private FakeUserDAO fakeUserDAO;
    private LoginHandler handler;
    private MockClientHandler mockClient;
    private static final Gson gson = new Gson();

    @BeforeEach
    void setUp() throws Exception {
        Field instanceField = AuctionService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        AuctionService existing = (AuctionService) instanceField.get(null);
        if (existing != null) existing.shutdown();
        instanceField.set(null, null);

        handler = new LoginHandler();
        fakeUserDAO = new FakeUserDAO();
        Field daoField = LoginHandler.class.getDeclaredField("userDAO");
        daoField.setAccessible(true);
        daoField.set(handler, fakeUserDAO);

        mockClient = new MockClientHandler("Unknown");
    }

    @Test
    @DisplayName("handle: returns LOGIN_SUCCESS for valid credentials with ACTIVE user")
    void should_returnLoginSuccess_when_validCredentialsAndActiveUser() {
        AuthPayload auth = new AuthPayload("alice", "alice@test.com", "pass123", "BIDDER");
        User activeUser = new Bidder("USER-001", "alice", "alice@test.com");

        fakeUserDAO.authUserId = "USER-001";
        fakeUserDAO.userById = activeUser;

        Request req = new Request(MessageType.LOGIN, "Unknown", gson.toJson(auth));
        Response res = handler.handle(req, mockClient);

        assertEquals(MessageType.LOGIN_SUCCESS, res.getType());
        assertEquals("SUCCESS", res.getStatus());
        assertNotNull(res.getPayload());
        assertEquals("USER-001", mockClient.getClientId());
        assertEquals("alice", fakeUserDAO.requestedAuthUsername);
        assertEquals("pass123", fakeUserDAO.requestedAuthPassword);
    }

    @Test
    @DisplayName("handle: returns LOGIN_FAIL when payload is null JSON (no auth object)")
    void should_returnLoginFail_when_payloadIsNull() {
        Request req = new Request(MessageType.LOGIN, "Unknown", "null");
        Response res = handler.handle(req, mockClient);

        assertEquals(MessageType.LOGIN_FAIL, res.getType());
        assertEquals("FAIL", res.getStatus());
        assertNull(fakeUserDAO.requestedAuthUsername);
    }

    @Test
    @DisplayName("handle: returns LOGIN_FAIL when username is null in payload")
    void should_returnLoginFail_when_usernameIsNull() {
        AuthPayload auth = new AuthPayload(null, "a@b.com", "pass", "BIDDER");
        Request req = new Request(MessageType.LOGIN, "Unknown", gson.toJson(auth));
        Response res = handler.handle(req, mockClient);

        assertEquals("FAIL", res.getStatus());
        assertNull(fakeUserDAO.requestedAuthUsername);
    }

    @Test
    @DisplayName("handle: returns LOGIN_FAIL when password is null in payload")
    void should_returnLoginFail_when_passwordIsNull() {
        AuthPayload auth = new AuthPayload("alice", "a@b.com", null, "BIDDER");
        Request req = new Request(MessageType.LOGIN, "Unknown", gson.toJson(auth));
        Response res = handler.handle(req, mockClient);

        assertEquals("FAIL", res.getStatus());
        assertNull(fakeUserDAO.requestedAuthUsername);
    }

    @Test
    @DisplayName("handle: returns LOGIN_FAIL when DAO returns null userId (wrong password)")
    void should_returnLoginFail_when_credentialsInvalid() {
        AuthPayload auth = new AuthPayload("alice", "a@b.com", "wrongPass", "BIDDER");
        fakeUserDAO.authUserId = null;

        Request req = new Request(MessageType.LOGIN, "Unknown", gson.toJson(auth));
        Response res = handler.handle(req, mockClient);

        assertEquals(MessageType.LOGIN_FAIL, res.getType());
        assertEquals("FAIL", res.getStatus());
        assertEquals("Invalid username or password", res.getMessage());
    }

    @Test
    @DisplayName("handle: returns LOGIN_FAIL with suspension message when account is BANNED")
    void should_returnLoginFail_when_userIsBanned() {
        AuthPayload auth = new AuthPayload("alice", "a@b.com", "pass123", "BIDDER");
        User bannedUser = new Bidder("USER-001", "alice", "alice@test.com", "BANNED");

        fakeUserDAO.authUserId = "USER-001";
        fakeUserDAO.userById = bannedUser;

        Request req = new Request(MessageType.LOGIN, "Unknown", gson.toJson(auth));
        Response res = handler.handle(req, mockClient);

        assertEquals(MessageType.LOGIN_FAIL, res.getType());
        assertEquals("FAIL", res.getStatus());
        assertTrue(res.getMessage().contains("suspended"));
        assertTrue(res.getMessage().contains("BANNED"));
        assertEquals("Unknown", mockClient.getClientId());
    }

    @Test
    @DisplayName("handle: returns LOGIN_FAIL when account status is SUSPENDED")
    void should_returnLoginFail_when_userIsSuspended() {
        AuthPayload auth = new AuthPayload("alice", "a@b.com", "pass123", "BIDDER");
        User suspendedUser = new Bidder("USER-001", "alice", "alice@test.com", "SUSPENDED");

        fakeUserDAO.authUserId = "USER-001";
        fakeUserDAO.userById = suspendedUser;

        Request req = new Request(MessageType.LOGIN, "Unknown", gson.toJson(auth));
        Response res = handler.handle(req, mockClient);

        assertEquals("FAIL", res.getStatus());
        assertTrue(res.getMessage().contains("SUSPENDED"));
    }

    @Test
    @DisplayName("handle: proceeds to LOGIN_SUCCESS even when getUserById returns null (degraded mode)")
    void should_returnLoginSuccess_when_userObjectNullButAuthValid() {
        AuthPayload auth = new AuthPayload("alice", "a@b.com", "pass123", "BIDDER");

        fakeUserDAO.authUserId = "USER-001";
        fakeUserDAO.userById = null;

        Request req = new Request(MessageType.LOGIN, "Unknown", gson.toJson(auth));
        Response res = handler.handle(req, mockClient);

        assertEquals(MessageType.LOGIN_SUCCESS, res.getType());
        assertEquals("USER-001", mockClient.getClientId());
    }

    static class MockClientHandler extends ClientHandler {
        private String id;
        MockClientHandler(String id) { super(null); this.id = id; }
        @Override public String getClientId() { return id; }
        @Override public void setClientId(String id) { this.id = id; }
        @Override public void sendResponse(Response r) { }
    }

    static class FakeUserDAO implements UserDAO {
        String authUserId;
        User userById;
        String requestedAuthUsername;
        String requestedAuthPassword;
        String requestedGetUserById;

        @Override
        public String authenticateUser(String emailOrUsername, String password) {
            this.requestedAuthUsername = emailOrUsername;
            this.requestedAuthPassword = password;
            return authUserId;
        }

        @Override
        public User getUserById(String id) {
            this.requestedGetUserById = id;
            return userById;
        }

        @Override public boolean addUser(User user, String password) { throw new UnsupportedOperationException(); }
        @Override public User getUserByUsername(String username) { throw new UnsupportedOperationException(); }
        @Override public java.util.List<User> getAllUsers() { throw new UnsupportedOperationException(); }
        @Override public int getUserCount() { throw new UnsupportedOperationException(); }
        @Override public boolean updateUserStatus(String userId, String status) { throw new UnsupportedOperationException(); }
    }
}
