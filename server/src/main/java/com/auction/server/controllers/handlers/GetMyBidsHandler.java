package com.auction.server.controllers.handlers;

import com.auction.server.controllers.CommandHandler;
import com.auction.server.dao.*;
import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.MyBidItemDTO;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.shared.models.BidTransaction;
import com.auction.shared.models.Item;
import com.google.gson.Gson;
import java.util.*;

/**
 * Handles GET_MY_BIDS requests from clients.
 * Combines watchlist items and placed bid history to calculate user-specific bid statuses.
 */
public class GetMyBidsHandler implements CommandHandler {
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final BidTransactionDAO bidTransactionDAO = new BidTransactionDAOImpl();
    private final WatchlistDAO watchlistDAO = new WatchlistDAOImpl();
    private static final Gson gson = new Gson();

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String userId = clientHandler.getClientId();
        if (userId == null || "Unknown".equals(userId)) {
            return new Response(MessageType.GET_MY_BIDS_RESPONSE, "FAIL", "Not authenticated", "[]");
        }

        // Retrieve all bid transactions by the user
        List<BidTransaction> allTransactions = bidTransactionDAO.getAllTransactions();
        Set<String> interactedItemIds = new LinkedHashSet<>();
        for (BidTransaction tx : allTransactions) {
            if (userId.equals(tx.getBidderId())) {
                interactedItemIds.add(tx.getItemId());
            }
        }

        // Retrieve watchlist items
        List<Item> watchlist = watchlistDAO.getWatchlist(userId);
        for (Item watchItem : watchlist) {
            interactedItemIds.add(watchItem.getId());
        }

        List<MyBidItemDTO> myBidItems = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (String itemId : interactedItemIds) {
            Item item = itemDAO.getItemById(itemId);
            if (item == null) continue;

            double maxBid = bidTransactionDAO.getMaxBidAmount(userId, itemId);
            double currentPrice = item.getCurrentHighestBid();
            long endTime = item.getEndTime();
            boolean isEnded = now >= endTime || "FINISHED".equals(item.getStatus()) || "CANCELED".equals(item.getStatus());

            String status;
            if (!isEnded) {
                if (maxBid == 0.0) {
                    status = "watching";
                } else {
                    if (userId.equals(item.getHighestBidderId())) {
                        status = "winning";
                    } else {
                        status = "outbid";
                    }
                }
            } else {
                if (maxBid == 0.0) {
                    status = "watching";
                } else {
                    if (userId.equals(item.getHighestBidderId())) {
                        status = "won";
                    } else {
                        status = "lost";
                    }
                }
            }

            myBidItems.add(new MyBidItemDTO(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getCategory().name(),
                maxBid,
                currentPrice,
                endTime,
                status
            ));
        }

        System.out.println("[GET_MY_BIDS] User " + userId + " fetched " + myBidItems.size() + " items.");
        return new Response(MessageType.GET_MY_BIDS_RESPONSE, "SUCCESS",
            myBidItems.size() + " items found", gson.toJson(myBidItems));
    }
}
