package com.auction.server.controllers.handlers;

import com.auction.server.controllers.CommandHandler;
import com.auction.server.dao.UserDAO;
import com.auction.server.dao.UserDAOImpl;
import com.auction.server.dao.WalletDAO;
import com.auction.server.dao.WalletDAOImpl;
import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.shared.models.User;
import com.auction.shared.models.TopupRequest;
import com.google.gson.Gson;
import java.util.List;

public class AdminWalletHandler implements CommandHandler {
    private final WalletDAO walletDAO = new WalletDAOImpl();
    private final UserDAO userDAO = new UserDAOImpl();
    private static final Gson gson = new Gson();

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String userId = clientHandler.getClientId();
        User user = userDAO.getUserById(userId);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return new Response(MessageType.BID_ERROR, "FAIL", "Access denied: Admin only", null);
        }

        if (request.getType() == MessageType.ADMIN_GET_PENDING_TOPUPS) {
            List<TopupRequest> pending = walletDAO.getPendingRequests();
            return new Response(MessageType.ADMIN_PENDING_TOPUPS_RESPONSE, "SUCCESS", "Requests fetched", gson.toJson(pending));
        } else if (request.getType() == MessageType.ADMIN_APPROVE_TOPUP) {
            String requestId = request.getPayload();
            List<TopupRequest> pending = walletDAO.getPendingRequests();
            TopupRequest target = null;
            for (TopupRequest tr : pending) {
                if (tr.id.equals(requestId)) {
                    target = tr;
                    break;
                }
            }

            if (target != null) {
                boolean statusUpdated = walletDAO.updateRequestStatus(requestId, "APPROVED");
                if (statusUpdated) {
                    walletDAO.updateBalance(target.userId, target.amount);
                    return new Response(MessageType.ADMIN_APPROVE_TOPUP, "SUCCESS", "Request approved", null);
                }
            }
            return new Response(MessageType.ADMIN_APPROVE_TOPUP, "FAIL", "Failed to approve request", null);
        } else if (request.getType() == MessageType.ADMIN_REJECT_TOPUP) {
            String requestId = request.getPayload();
            boolean success = walletDAO.updateRequestStatus(requestId, "REJECTED");
            return new Response(MessageType.ADMIN_REJECT_TOPUP, success ? "SUCCESS" : "FAIL", 
                success ? "Request rejected" : "Failed to reject request", null);
        }

        return new Response(MessageType.BID_ERROR, "FAIL", "Unknown admin command", null);
    }
}
