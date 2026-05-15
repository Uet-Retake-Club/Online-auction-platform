package com.auction.server.controllers.handlers;

import com.auction.server.controllers.CommandHandler;
import com.auction.server.dao.WalletDAO;
import com.auction.server.dao.WalletDAOImpl;
import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;

public class WalletHandler implements CommandHandler {
    private final WalletDAO walletDAO = new WalletDAOImpl();

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String userId = clientHandler.getClientId();
        if (userId == null || "Unknown".equals(userId)) {
            return new Response(MessageType.BID_ERROR, "FAIL", "Unauthorized", null);
        }

        if (request.getType() == MessageType.GET_WALLET_BALANCE) {
            double balance = walletDAO.getBalance(userId);
            return new Response(MessageType.WALLET_BALANCE_RESPONSE, "SUCCESS", "Balance fetched", String.valueOf(balance));
        } else if (request.getType() == MessageType.WALLET_TOPUP_REQUEST) {
            try {
                double amount = Double.parseDouble(request.getPayload());
                boolean success = walletDAO.createTopupRequest(userId, amount);
                return new Response(MessageType.WALLET_TOPUP_APPROVE, success ? "SUCCESS" : "FAIL", 
                    success ? "Top-up request submitted" : "Failed to submit request", null);
            } catch (Exception e) {
                return new Response(MessageType.BID_ERROR, "FAIL", "Invalid amount", null);
            }
        }

        return new Response(MessageType.BID_ERROR, "FAIL", "Unknown wallet command", null);
    }
}
