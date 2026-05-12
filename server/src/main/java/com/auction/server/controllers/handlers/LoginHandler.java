package com.auction.server.controllers.handlers;

import com.auction.server.controllers.CommandHandler;
import com.auction.server.network.ClientHandler;
import com.auction.server.services.AuctionManager;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;

public class LoginHandler implements CommandHandler {
    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        clientHandler.setClientId(request.getSenderId());
        AuctionManager.getInstance().registerClient(request.getSenderId(), clientHandler);
        
        // Tự động gửi trạng thái hiện tại (Giống code cũ của bạn)
        clientHandler.sendResponse(AuctionManager.getInstance().getCurrentStatusResponse());
        
        return new Response(MessageType.LOGIN, "SUCCESS", "Đăng nhập Socket thành công", null);
    }
}