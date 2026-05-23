package com.auction.server.controllers.handlers;

import com.auction.server.controllers.CommandHandler;
import com.auction.server.dao.BidTransactionDAO;
import com.auction.server.dao.BidTransactionDAOImpl;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.ItemDAOImpl;
import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.shared.models.BidTransaction;
import com.auction.shared.models.Item;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetSellerDashboardHandler implements CommandHandler {
    // Instantiate DAOs to interact with SQLite
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final BidTransactionDAO bidTransactionDAO = new BidTransactionDAOImpl();
    private final Gson gson = new Gson();

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        // Retrieve the current logged-in seller's ID
        String sellerId = clientHandler.getClientId();
        
        // 1. Authentication check
        if (sellerId == null || "Unknown".equals(sellerId)) {
            return new Response(MessageType.BID_ERROR, "FAIL", "Authentication required to view dashboard.", null);
        }

        try {
            // 2. Fetch all items posted by this seller
            List<Item> sellerItems = itemDAO.getItemsBySellerId(sellerId);
            
            // 3. Prepare a list to hold the combined statistics
            // Each Map will contain the "item" object and its corresponding "totalBids" count
            List<Map<String, Object>> dashboardData = new ArrayList<>();
            
            // 4. Loop through each item to calculate its hotness (bid count)
            for (Item item : sellerItems) {
                Map<String, Object> itemStat = new HashMap<>();
                itemStat.put("item", item); // Put the full item details
                
                // Fetch the bid history for this specific item
                List<BidTransaction> bidHistory = bidTransactionDAO.getHistoryByItem(item.getId());
                
                // Add the total number of bids to the statistics map
                itemStat.put("totalBids", bidHistory.size()); 
                
                dashboardData.add(itemStat);
            }
            
            // 5. Serialize the dashboard data into a JSON string
            String jsonPayload = gson.toJson(dashboardData);
            
            // 6. Return successful response with the payload
            return new Response(MessageType.valueOf("GET_SELLER_DASHBOARD_SUCCESS"), "SUCCESS", "Dashboard retrieved successfully", jsonPayload);
            
        } catch (Exception e) {
            System.err.println(" [ERROR] Failed to fetch dashboard for seller: " + sellerId);
            e.printStackTrace();
            return new Response(MessageType.BID_ERROR, "FAIL", "Internal server error while fetching dashboard.", null);
        }
    }
}