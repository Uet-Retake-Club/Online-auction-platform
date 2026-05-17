package com.auction.server.controllers.handlers;

import com.auction.server.controllers.CommandHandler;
import com.auction.server.network.ClientHandler;
import com.auction.server.services.AuctionService;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.shared.models.AutoBidSettings;
import com.google.gson.Gson;

public class SetupAutoBidHandler implements CommandHandler {
    private static final Gson gson = new Gson();

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String authenticatedUserId = clientHandler.getClientId();
        if ("Unknown".equals(authenticatedUserId)) {
            return new Response(com.auction.shared.dto.MessageType.SETUP_AUTO_BID, "FAIL", "Bạn cần đăng nhập để thực hiện hành động này.", null);
        }

        AutoBidSettings settings = gson.fromJson(request.getPayload(), AutoBidSettings.class);
        // Force settings to use authenticated user ID
        settings.setBidderId(authenticatedUserId); 
        return AuctionService.getInstance().registerAutoBid(settings);
    }
}