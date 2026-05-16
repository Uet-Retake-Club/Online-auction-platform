package com.auction.server.controllers.handlers;

import com.auction.server.controllers.CommandHandler;
import com.auction.server.dao.UserDAO;
import com.auction.server.dao.UserDAOImpl;
import com.auction.server.network.ClientHandler;
import com.auction.server.services.AuctionService;
import com.auction.shared.dto.AuthPayload;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.google.gson.Gson;

public class LoginHandler implements CommandHandler {
    private final UserDAO userDAO = new UserDAOImpl();
    private static final Gson gson = new Gson();

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        AuthPayload auth = gson.fromJson(request.getPayload(), AuthPayload.class);
        
        if (auth == null || auth.getUsername() == null || auth.getPassword() == null) {
            return new Response(MessageType.LOGIN_FAIL, "FAIL", "Invalid login data", null);
        }

        String userId = userDAO.authenticateUser(auth.getUsername(), auth.getPassword());
        
        if (userId != null) {
            clientHandler.setClientId(userId);
            AuctionService.getInstance().registerClient(userId, clientHandler);
            com.auction.shared.models.User user = userDAO.getUserById(userId);
            return new Response(MessageType.LOGIN_SUCCESS, "SUCCESS", "Login successful", gson.toJson(user));
        } else {
            return new Response(MessageType.LOGIN_FAIL, "FAIL", "Invalid username or password", null);
        }
    }
}