package com.auction.server.controllers.handlers;

import static org.junit.jupiter.api.Assertions.*;

import com.auction.server.dao.UserDAO;
import com.auction.server.network.ClientHandler;
import com.auction.server.services.AuctionService;
import com.auction.shared.dto.AuthPayload;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.shared.models.Admin;
import com.auction.shared.models.Bidder;
import com.auction.shared.models.Seller;
import com.auction.shared.models.User;
import com.google.gson.Gson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Field;

@DisplayName("RegisterHandler — Unit Tests")
class RegisterHandlerTest {

    static {
        System.setProperty("testMode", "true");
    }

    private FakeUserDAO fakeUserDAO;
    private RegisterHandler handler;
    private MockClientHandler mockClient;
    private static final Gson gson = new Gson();

    @BeforeEach
    void setUp() throws Exception {
        Field instanceField = AuctionService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        AuctionService existing = (AuctionService) instanceField.get(null);
        if (existing != null) existing.shutdown();
        instanceField.set(null, null);

        handler = new RegisterHandler();
        fakeUserDAO = new FakeUserDAO();
        Field daoField = RegisterHandler.class.getDeclaredField("userDAO");
        daoField.setAccessible(true);
        daoField.set(handler, fakeUserDAO);

        mockClient = new MockClientHandler("Unknown");
    }

    @ParameterizedTest(name = "role={0} → creates {1}")
    @CsvSource({
        "BIDDER, class com.auction.shared.models.Bidder",
        "SELLER, class com.auction.shared.models.Seller",
        "ADMIN,  class com.auction.shared.models.Admin"
    })
    @DisplayName("handle: creates correct User subclass based on role")
    void should_createCorrectUserSubclass_when_roleProvided(String role, String expectedClass) {
        AuthPayload auth = new AuthPayload("testuser", "test@x.com", "password", role);
        fakeUserDAO.addUserResult = true;

        Request req = new Request(MessageType.REGISTER, "Unknown", gson.toJson(auth));
        Response res = handler.handle(req, mockClient);

        assertEquals("SUCCESS", res.getStatus());
        assertNotNull(fakeUserDAO.addedUser);
        assertEquals("password", fakeUserDAO.addedPassword);
        assertEquals(expectedClass, fakeUserDAO.addedUser.getClass().toString());
    }

    @Test
    @DisplayName("handle: returns REGISTER_SUCCESS and sets clientHandler ID on successful registration")
    void should_setClientIdAndReturnSuccess_when_registrationSucceeds() {
        AuthPayload auth = new AuthPayload("alice", "alice@test.com", "pass", "BIDDER");
        fakeUserDAO.addUserResult = true;

        Request req = new Request(MessageType.REGISTER, "Unknown", gson.toJson(auth));
        Response res = handler.handle(req, mockClient);

        assertEquals(MessageType.REGISTER_SUCCESS, res.getType());
        assertEquals("SUCCESS", res.getStatus());
        assertNotEquals("Unknown", mockClient.getClientId());
        assertNotNull(res.getPayload());
        assertTrue(res.getPayload().startsWith("USER-"));
    }

    @Test
    @DisplayName("handle: uses {username}@placeholder.com when email is null")
    void should_usePlaceholderEmail_when_emailIsNull() {
        AuthPayload auth = new AuthPayload("alice", null, "pass", "BIDDER");
        fakeUserDAO.addUserResult = true;

        Request req = new Request(MessageType.REGISTER, "Unknown", gson.toJson(auth));
        handler.handle(req, mockClient);

        assertNotNull(fakeUserDAO.addedUser);
        assertEquals("alice@placeholder.com", fakeUserDAO.addedUser.getEmail());
    }

    @Test
    @DisplayName("handle: returns REGISTER_FAIL when payload parses to null")
    void should_returnFail_when_payloadIsNull() {
        Request req = new Request(MessageType.REGISTER, "Unknown", "null");
        Response res = handler.handle(req, mockClient);

        assertEquals(MessageType.REGISTER_FAIL, res.getType());
        assertEquals("FAIL", res.getStatus());
        assertNull(fakeUserDAO.addedUser);
    }

    @Test
    @DisplayName("handle: returns REGISTER_FAIL when username is null")
    void should_returnFail_when_usernameIsNull() {
        AuthPayload auth = new AuthPayload(null, "a@b.com", "pass", "BIDDER");
        Request req = new Request(MessageType.REGISTER, "Unknown", gson.toJson(auth));
        Response res = handler.handle(req, mockClient);

        assertEquals("FAIL", res.getStatus());
        assertNull(fakeUserDAO.addedUser);
    }

    @Test
    @DisplayName("handle: returns REGISTER_FAIL when password is null")
    void should_returnFail_when_passwordIsNull() {
        AuthPayload auth = new AuthPayload("alice", "a@b.com", null, "BIDDER");
        Request req = new Request(MessageType.REGISTER, "Unknown", gson.toJson(auth));
        Response res = handler.handle(req, mockClient);

        assertEquals("FAIL", res.getStatus());
        assertNull(fakeUserDAO.addedUser);
    }

    @Test
    @DisplayName("handle: returns REGISTER_FAIL when role is null")
    void should_returnFail_when_roleIsNull() {
        AuthPayload auth = new AuthPayload("alice", "a@b.com", "pass", null);
        Request req = new Request(MessageType.REGISTER, "Unknown", gson.toJson(auth));
        Response res = handler.handle(req, mockClient);

        assertEquals("FAIL", res.getStatus());
        assertNull(fakeUserDAO.addedUser);
    }

    @Test
    @DisplayName("handle: returns REGISTER_FAIL when DAO addUser fails (e.g., duplicate username)")
    void should_returnFail_when_daoAddUserFails() {
        AuthPayload auth = new AuthPayload("alice", "alice@test.com", "pass", "BIDDER");
        fakeUserDAO.addUserResult = false;

        Request req = new Request(MessageType.REGISTER, "Unknown", gson.toJson(auth));
        Response res = handler.handle(req, mockClient);

        assertEquals(MessageType.REGISTER_FAIL, res.getType());
        assertEquals("FAIL", res.getStatus());
        assertTrue(res.getMessage().contains("already exists") || res.getMessage().contains("DB error"));
        assertEquals("Unknown", mockClient.getClientId());
    }

    @Test
    @DisplayName("handle: unknown role falls back to Bidder (default case)")
    void should_createBidder_when_roleIsUnknown() {
        AuthPayload auth = new AuthPayload("alice", "a@b.com", "pass", "UNKNOWN_ROLE");
        fakeUserDAO.addUserResult = true;

        Request req = new Request(MessageType.REGISTER, "Unknown", gson.toJson(auth));
        Response res = handler.handle(req, mockClient);

        assertEquals("SUCCESS", res.getStatus());
        assertNotNull(fakeUserDAO.addedUser);
        assertTrue(fakeUserDAO.addedUser instanceof Bidder);
    }

    static class MockClientHandler extends ClientHandler {
        private String id;
        MockClientHandler(String id) { super(null); this.id = id; }
        @Override public String getClientId() { return id; }
        @Override public void setClientId(String id) { this.id = id; }
        @Override public void sendResponse(Response r) { }
    }

    static class FakeUserDAO implements UserDAO {
        User addedUser;
        String addedPassword;
        boolean addUserResult = true;

        @Override
        public boolean addUser(User user, String password) {
            this.addedUser = user;
            this.addedPassword = password;
            return addUserResult;
        }

        @Override public User getUserById(String id) { throw new UnsupportedOperationException(); }
        @Override public User getUserByUsername(String username) { throw new UnsupportedOperationException(); }
        @Override public String authenticateUser(String emailOrUsername, String password) { throw new UnsupportedOperationException(); }
        @Override public java.util.List<User> getAllUsers() { throw new UnsupportedOperationException(); }
        @Override public int getUserCount() { throw new UnsupportedOperationException(); }
        @Override public boolean updateUserStatus(String userId, String status) { throw new UnsupportedOperationException(); }
    }
}
