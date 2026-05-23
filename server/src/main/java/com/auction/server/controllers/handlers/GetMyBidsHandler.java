package com.auction.server.controllers.handlers;

import com.auction.server.controllers.CommandHandler;
import com.auction.server.dao.BidTransactionDAO;
import com.auction.server.dao.BidTransactionDAOImpl;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.ItemDAOImpl;
import com.auction.server.dao.WatchlistDAO;
import com.auction.server.dao.WatchlistDAOImpl;
import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.shared.dto.UserBidDTO;
import com.auction.shared.models.Item;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GetMyBidsHandler implements CommandHandler {
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final BidTransactionDAO bidDAO = new BidTransactionDAOImpl();
    private final WatchlistDAO watchlistDAO = new WatchlistDAOImpl();
    private static final Gson gson = new Gson();

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String userId = clientHandler.getClientId();
        if (userId == null || "Unknown".equals(userId)) {
            return new Response(MessageType.MY_BIDS_RESPONSE, "FAIL", "Not authenticated", "[]");
        }

        try {
            // Get bidded item IDs
            List<String> biddedItemIds = bidDAO.getBiddedItemIds(userId);
            
            // Get watchlist items
            List<Item> watchlistItems = watchlistDAO.getWatchlist(userId);
            Set<String> watchlistIds = new HashSet<>();
            for (Item wItem : watchlistItems) {
                watchlistIds.add(wItem.getId());
            }

            // Combine unique item IDs
            Set<String> allItemIds = new HashSet<>(biddedItemIds);
            allItemIds.addAll(watchlistIds);

            List<UserBidDTO> userBidDTOs = new ArrayList<>();
            for (String itemId : allItemIds) {
                Item item = itemDAO.getItemById(itemId);
                if (item == null) {
                    continue;
                }

                double myHighestBid = bidDAO.getMaxBidAmount(userId, itemId);
                boolean isWatchlisted = watchlistIds.contains(itemId);

                UserBidDTO dto = new UserBidDTO(
                    item.getId(),
                    item.getName(),
                    item.getCategory() != null ? item.getCategory().name() : "OTHER",
                    item.getDescription(),
                    item.getStartingPrice(),
                    item.getCurrentHighestBid(),
                    item.getHighestBidderId(),
                    item.getEndTime(),
                    item.getStatus() != null ? item.getStatus() : "OPEN",
                    myHighestBid,
                    isWatchlisted
                );
                userBidDTOs.add(dto);
            }

            return new Response(MessageType.MY_BIDS_RESPONSE, "SUCCESS", "User bids fetched successfully", gson.toJson(userBidDTOs));
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(MessageType.MY_BIDS_RESPONSE, "FAIL", "Internal server error: " + e.getMessage(), "[]");
        }
    }
}
