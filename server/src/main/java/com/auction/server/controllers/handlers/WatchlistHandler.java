package com.auction.server.controllers.handlers;

import com.auction.server.controllers.CommandHandler;
import com.auction.server.dao.WatchlistDAO;
import com.auction.server.dao.WatchlistDAOImpl;
import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.google.gson.Gson;

public class WatchlistHandler implements CommandHandler {
    private final WatchlistDAO watchlistDAO = new WatchlistDAOImpl();
    private static final Gson gson = new Gson();

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String userId = clientHandler.getClientId();
        if (userId == null || "Unknown".equals(userId)) {
            return new Response(MessageType.BID_ERROR, "FAIL", "Unauthorized", null);
        }

        if (request.getType() == MessageType.WATCHLIST_ADD) {
            boolean success = watchlistDAO.addToWatchlist(userId, request.getPayload());
            return new Response(MessageType.WATCHLIST_RESPONSE, success ? "SUCCESS" : "FAIL", 
                success ? "Added to watchlist" : "Failed to add", null);
        } else if (request.getType() == MessageType.WATCHLIST_REMOVE) {
            boolean success = watchlistDAO.removeFromWatchlist(userId, request.getPayload());
            return new Response(MessageType.WATCHLIST_RESPONSE, success ? "SUCCESS" : "FAIL", 
                success ? "Removed from watchlist" : "Failed to remove", null);
        } else if (request.getType() == MessageType.GET_WATCHLIST) {
            return new Response(MessageType.WATCHLIST_RESPONSE, "SUCCESS", "Watchlist fetched", 
                gson.toJson(watchlistDAO.getWatchlist(userId)));
        }

        return new Response(MessageType.BID_ERROR, "FAIL", "Unknown watchlist command", null);
    }
}
