package com.auction.server.controllers.handlers;

import com.auction.server.controllers.CommandHandler;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.ItemDAOImpl;
import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.shared.models.Item;
import java.util.List;

/**
 * Handles GET_SELLER_ITEMS requests.
 * Returns all items created by the authenticated seller as a JSON array.
 */
public class GetSellerItemsHandler implements CommandHandler {
    private final ItemDAO itemDAO = new ItemDAOImpl();

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String sellerId = clientHandler.getClientId();
        if (sellerId == null || "Unknown".equals(sellerId)) {
            return new Response(MessageType.GET_SELLER_ITEMS_RESPONSE, "FAIL",
                "Not authenticated", "[]");
        }

        try {
            List<Item> items = itemDAO.getItemsBySellerId(sellerId);

            // Build a simplified JSON array for the client
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < items.size(); i++) {
                Item item = items.get(i);
                if (i > 0) sb.append(",");
                sb.append("{");
                sb.append("\"id\":\"").append(item.getId() != null ? item.getId() : "").append("\",");
                sb.append("\"name\":\"").append(escapeJson(item.getName())).append("\",");
                sb.append("\"description\":\"").append(escapeJson(item.getDescription())).append("\",");
                sb.append("\"category\":\"").append(item.getCategory() != null ? item.getCategory().name() : "OTHER").append("\",");
                sb.append("\"startPrice\":").append(item.getStartingPrice()).append(",");
                sb.append("\"currentPrice\":").append(item.getCurrentHighestBid()).append(",");
                sb.append("\"highestBidderId\":").append(item.getHighestBidderId() != null ? "\"" + item.getHighestBidderId() + "\"" : "null").append(",");
                sb.append("\"startTime\":").append(item.getStartTime()).append(",");
                sb.append("\"endTime\":").append(item.getEndTime()).append(",");
                sb.append("\"status\":\"").append(item.getStatus() != null ? item.getStatus() : "OPEN").append("\",");
                String base64Image = "";
                if (item.getImageData() != null && item.getImageData().length > 0) {
                    base64Image = java.util.Base64.getEncoder().encodeToString(item.getImageData());
                }
                sb.append("\"imageData\":\"").append(base64Image).append("\"");
                sb.append("}");
            }
            sb.append("]");

            System.out.println("[GET_SELLER_ITEMS] Returning " + items.size() + " items for seller " + sellerId);
            return new Response(MessageType.GET_SELLER_ITEMS_RESPONSE, "SUCCESS",
                items.size() + " items found", sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(MessageType.GET_SELLER_ITEMS_RESPONSE, "FAIL",
                "Internal server error: " + e.getMessage(), "[]");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
