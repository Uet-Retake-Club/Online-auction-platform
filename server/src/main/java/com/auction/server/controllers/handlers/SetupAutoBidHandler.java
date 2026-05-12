package com.auction.server.controllers.handlers;

import com.auction.server.controllers.CommandHandler;
import com.auction.server.network.ClientHandler;
import com.auction.server.services.AuctionManager;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.shared.models.AutoBidSettings;
import com.google.gson.Gson;

public class SetupAutoBidHandler implements CommandHandler {
    private static final Gson gson = new Gson();

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        AutoBidSettings settings = gson.fromJson(request.getPayload(), AutoBidSettings.class);
        return AuctionManager.getInstance().registerAutoBid(settings);
    }
}