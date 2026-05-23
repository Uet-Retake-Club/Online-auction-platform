package com.auction.server.controllers.handlers;

import com.auction.server.controllers.CommandHandler;
import com.auction.server.dao.InvoiceDAO;
import com.auction.server.dao.InvoiceDAOImpl;
import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.shared.models.Invoice;
import com.google.gson.Gson;

import java.util.List;

public class GetMyInvoicesHandler implements CommandHandler {
    // Instantiate the DAO to interact with SQLite
    private final InvoiceDAO invoiceDAO = new InvoiceDAOImpl();
    private final Gson gson = new Gson();

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        // Retrieve the current logged-in user's ID
        String userId = clientHandler.getClientId();
        
        // 1. Authentication check
        if (userId == null || "Unknown".equals(userId)) {
            return new Response(MessageType.BID_ERROR, "FAIL", "Authentication required to view invoices.", null);
        }

        try {
            // 2. Fetch all invoices where the user is either a Bidder (Buyer) or Seller
            List<Invoice> myInvoices = invoiceDAO.getInvoicesByUserId(userId);
            
            // 3. Serialize the list of invoices into a JSON string
            String jsonPayload = gson.toJson(myInvoices);
            
            // 4. Return successful response with the payload
            // Note: Make sure GET_MY_INVOICES_SUCCESS exists in MessageType enum
            return new Response(MessageType.valueOf("GET_MY_INVOICES_SUCCESS"), "SUCCESS", "Invoices retrieved successfully", jsonPayload);
            
        } catch (Exception e) {
            System.err.println(" [ERROR] Failed to fetch invoices for user: " + userId);
            e.printStackTrace();
            return new Response(MessageType.BID_ERROR, "FAIL", "Internal server error while fetching invoices.", null);
        }
    }
}