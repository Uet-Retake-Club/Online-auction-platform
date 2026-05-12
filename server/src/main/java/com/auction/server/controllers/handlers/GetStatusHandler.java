package com.auction.server.controllers.handlers;

import com.auction.server.controllers.CommandHandler;
import com.auction.server.network.ClientHandler;
import com.auction.server.services.AuctionService;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;

public class GetStatusHandler implements CommandHandler {
    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        return AuctionService.getInstance().getCurrentStatusResponse();
    }
}