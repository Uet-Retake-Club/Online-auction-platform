package com.auction.server.controllers.handlers;

import com.auction.server.controllers.CommandHandler;
import com.auction.server.dao.UserDAO;
import com.auction.server.dao.UserDAOImpl;
import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.AuthPayload;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
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
        boolean success = userDAO.registerUser(newUserId, auth.getUsername(), auth.getPassword(), auth.getRole());
        
        if (success) {
            return new Response(MessageType.REGISTER_SUCCESS, "SUCCESS", "Đăng ký thành công", newUserId);
        } else {
            return new Response(MessageType.REGISTER_FAIL, "FAIL", "Tên đăng nhập đã tồn tại hoặc lỗi DB", null);
        }
    }
}
