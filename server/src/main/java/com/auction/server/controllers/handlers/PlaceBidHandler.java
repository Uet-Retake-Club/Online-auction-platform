package com.auction.server.controllers.handlers;

import com.auction.server.controllers.CommandHandler;
import com.auction.server.network.ClientHandler;
import com.auction.server.services.AuctionService;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.shared.models.BidTransaction;
import com.google.gson.Gson;

public class PlaceBidHandler implements CommandHandler {
    private static final Gson gson = new Gson();

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        BidTransaction bidTx = gson.fromJson(request.getPayload(), BidTransaction.class);
        return AuctionService.getInstance().processBid(
                request.getSenderId(), bidTx.getBidAmount(), request.getPayload());
    }
}