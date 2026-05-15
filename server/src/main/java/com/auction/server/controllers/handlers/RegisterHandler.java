package com.auction.server.controllers.handlers;

import com.auction.server.controllers.CommandHandler;
import com.auction.server.dao.UserDAO;
import com.auction.server.dao.UserDAOImpl;
import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.AuthPayload;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.shared.models.Admin;
import com.auction.shared.models.Bidder;
import com.auction.shared.models.Seller;
import com.auction.shared.models.User;
import com.google.gson.Gson;
import java.util.UUID;

public class RegisterHandler implements CommandHandler {
    private final UserDAO userDAO = new UserDAOImpl();
    private static final Gson gson = new Gson();

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        AuthPayload auth = gson.fromJson(request.getPayload(), AuthPayload.class);
        
        if (auth == null || auth.getUsername() == null || auth.getPassword() == null || auth.getRole() == null) {
            return new Response(MessageType.REGISTER_FAIL, "FAIL", "Invalid registration data", null);
        }

        String newUserId = "USER-" + UUID.randomUUID().toString().substring(0, 8);
        String email = auth.getEmail() != null ? auth.getEmail() : auth.getUsername() + "@placeholder.com";
        
        User user;
        if ("ADMIN".equals(auth.getRole())) user = new Admin(newUserId, auth.getUsername(), email);
        else if ("SELLER".equals(auth.getRole())) user = new Seller(newUserId, auth.getUsername(), email);
        else user = new Bidder(newUserId, auth.getUsername(), email);

        boolean success = userDAO.addUser(user, auth.getPassword());

        if (success) {
            // Auto-login: Register client in SessionManager immediately
            clientHandler.setClientId(newUserId);
            com.auction.server.services.AuctionService.getInstance().registerClient(newUserId, clientHandler);
            
            return new Response(MessageType.REGISTER_SUCCESS, "SUCCESS", "Registration successful", newUserId);
        } else {
            return new Response(MessageType.REGISTER_FAIL, "FAIL", "Username already exists or DB error", null);
        }
    }
}
