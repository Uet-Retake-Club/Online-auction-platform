package com.auction.server.controllers.handlers;

import com.auction.server.controllers.CommandHandler;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.ItemDAOImpl;
import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.shared.models.Electronics;
import com.auction.shared.models.ItemCategory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.UUID;

/**
 * Handles CREATE_ITEM requests from sellers.
 * Parses the item JSON, persists it to DB, and returns success/fail.
 */
public class CreateItemHandler implements CommandHandler {
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private static final Gson gson = new Gson();

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String sellerId = clientHandler.getClientId();
        if (sellerId == null || "Unknown".equals(sellerId)) {
            return new Response(MessageType.CREATE_ITEM_FAIL, "FAIL",
                "You must be logged in to create a listing", null);
        }

        try {
            JsonObject json = gson.fromJson(request.getPayload(), JsonObject.class);
            if (json == null) {
                return new Response(MessageType.CREATE_ITEM_FAIL, "FAIL",
                    "Invalid item data", null);
            }

            String title = json.has("title") ? json.get("title").getAsString() : null;
            String categoryStr = json.has("category") ? json.get("category").getAsString() : null;
            String description = json.has("description") ? json.get("description").getAsString() : "";
            double startPrice = json.has("startPrice") ? json.get("startPrice").getAsDouble() : 0;
            long startTime = json.has("startTime") ? json.get("startTime").getAsLong() : System.currentTimeMillis();
            long endTime = json.has("endTime") ? json.get("endTime").getAsLong() : (System.currentTimeMillis() + 7 * 24 * 3600 * 1000L);

            if (title == null || title.trim().isEmpty() || categoryStr == null) {
                return new Response(MessageType.CREATE_ITEM_FAIL, "FAIL",
                    "Title and category are required", null);
            }

            // Map category string to enum
            ItemCategory category;
            try {
                category = ItemCategory.valueOf(categoryStr.toUpperCase().replace(" & ", "_AND_").replace(" ", "_"));
            } catch (IllegalArgumentException e) {
                category = ItemCategory.ELECTRONICS; // fallback
            }

            String itemId = "ITEM-" + UUID.randomUUID().toString().substring(0, 8);

            // Use Electronics as a generic concrete subclass
            Electronics item = new Electronics(itemId, title, description,
                startPrice, startTime, endTime, "", "", sellerId);
            item.setCategory(category);
            item.setStatus("OPEN");
            item.setCurrentHighestBid(startPrice);

            boolean success = itemDAO.addItem(item);
            if (success) {
                System.out.println("[CREATE_ITEM] New listing: " + title + " (ID: " + itemId + ") by " + sellerId);
                return new Response(MessageType.CREATE_ITEM_SUCCESS, "SUCCESS",
                    "Listing created successfully", itemId);
            } else {
                return new Response(MessageType.CREATE_ITEM_FAIL, "FAIL",
                    "Failed to save item to database", null);
            }

        } catch (Exception e) {
            System.err.println("[CREATE_ITEM] Error: " + e.getMessage());
            return new Response(MessageType.CREATE_ITEM_FAIL, "FAIL",
                "Server error: " + e.getMessage(), null);
        }
    }
}
