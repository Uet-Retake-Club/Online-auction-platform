package com.auction.server.controllers;

import com.auction.server.controllers.handlers.*;
import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;

import java.util.HashMap;
import java.util.Map;

public class RequestDispatcher {
    private final Map<MessageType, CommandHandler> handlerRegistry = new HashMap<>();

    public RequestDispatcher() {
        // Đăng ký các Lệnh vào Từ điển
        handlerRegistry.put(MessageType.LOGIN, new LoginHandler());
        handlerRegistry.put(MessageType.PLACE_BID, new PlaceBidHandler());
        handlerRegistry.put(MessageType.SETUP_AUTO_BID, new SetupAutoBidHandler());
        handlerRegistry.put(MessageType.GET_STATUS, new GetStatusHandler());
    }

    public Response dispatch(Request request, ClientHandler clientHandler) {
        CommandHandler handler = handlerRegistry.get(request.getType());
        
        if (handler != null) {
            return handler.handle(request, clientHandler);
        } else {
            System.out.println(" [DISPATCHER] Không tìm thấy Handler cho lệnh: " + request.getType());
            return null; // Trả về null nếu không có handler
        }
    }
}