package com.auction.server.controllers.handlers;

import com.auction.server.controllers.CommandHandler;
import com.auction.server.dao.WalletDAO;
import com.auction.server.dao.WalletDAOImpl;
import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class AdminWalletHandler implements CommandHandler {
    private final WalletDAO walletDAO = new WalletDAOImpl();
    private static final Gson gson = new Gson();

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        // Simple role check: id starts with ADMIN
        String userId = clientHandler.getClientId();
        if (userId == null || !userId.startsWith("ADMIN")) {
            return new Response(MessageType.BID_ERROR, "FAIL", "Admin only", null);
        }

        if (request.getType() == MessageType.WALLET_TOPUP_APPROVE) {
            JsonObject json = gson.fromJson(request.getPayload(), JsonObject.class);
            String requestId = json.get("requestId").getAsString();
            String status = json.get("status").getAsString(); // APPROVED or REJECTED
            String targetUserId = json.get("userId").getAsString();
            double amount = json.get("amount").getAsDouble();

            boolean success = walletDAO.updateRequestStatus(requestId, status);
            if (success && "APPROVED".equals(status)) {
                walletDAO.updateBalance(targetUserId, amount);
            }

            return new Response(MessageType.WALLET_TOPUP_APPROVE, success ? "SUCCESS" : "FAIL", 
                "Request " + status, null);
        }

        return new Response(MessageType.BID_ERROR, "FAIL", "Unknown admin command", null);
    }
}
