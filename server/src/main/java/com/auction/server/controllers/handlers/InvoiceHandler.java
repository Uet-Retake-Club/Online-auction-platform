package com.auction.server.controllers.handlers;

import com.auction.server.controllers.CommandHandler;
import com.auction.server.network.ClientHandler;
import com.auction.server.services.AuctionService;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class InvoiceHandler implements CommandHandler {
    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String userId = clientHandler.getClientId();
        if (userId == null || "Unknown".equals(userId)) {
            return new Response(MessageType.BID_ERROR, "FAIL", "Authentication required!", null);
        }

        try {
            // Extract invoice ID from payload (Assuming Client sends JSON: {"invoiceId": "INV-1234"})
            JsonObject json = JsonParser.parseString(request.getPayload()).getAsJsonObject();
            String invoiceId = json.get("invoiceId").getAsString();

            if (request.getType().name().equals("INVOICE_PAY")) {
                return AuctionService.getInstance().processPayment(invoiceId, userId);
            } else if (request.getType().name().equals("INVOICE_CANCEL")) {
                return AuctionService.getInstance().processCancellation(invoiceId, userId);
            }
        } catch (Exception e) {
            return new Response(MessageType.BID_ERROR, "FAIL", "Invalid invoice data format", null);
        }

        return new Response(MessageType.BID_ERROR, "FAIL", "Invalid command type", null);
    }
}