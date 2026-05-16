package com.auction.server.controllers.handlers;

import com.auction.server.controllers.CommandHandler;
import com.auction.server.dao.*;
import com.auction.server.network.ClientHandler;
import com.auction.shared.dto.*;
import com.auction.shared.models.Item;
import com.auction.shared.models.TopupRequest;
import com.auction.shared.models.User;
import com.google.gson.Gson;
import java.util.List;

public class AdminHandler implements CommandHandler {
    private final UserDAO userDAO = new UserDAOImpl();
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final BidTransactionDAO bidDAO = new BidTransactionDAOImpl();
    private final InvoiceDAO invoiceDAO = new InvoiceDAOImpl();
    private final WalletDAO walletDAO = new WalletDAOImpl();
    private static final Gson gson = new Gson();

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String userId = clientHandler.getClientId();
        User admin = userDAO.getUserById(userId);
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            return new Response(request.getType(), "FAIL", "Access denied: Admin only", null);
        }

        return switch (request.getType()) {
            case ADMIN_GET_STATS -> getStats();
            case ADMIN_GET_USERS -> getUsers();
            case ADMIN_GET_AUCTIONS -> getAuctions();
            case ADMIN_GET_PENDING_TOPUPS -> getPendingTopups();
            case ADMIN_APPROVE_TOPUP -> approveTopup(request.getPayload());
            case ADMIN_REJECT_TOPUP -> rejectTopup(request.getPayload());
            default -> new Response(request.getType(), "FAIL", "Unknown admin command", null);
        };
    }

    private Response getStats() {
        int totalUsers = userDAO.getUserCount();
        int activeAuctions = itemDAO.getActiveAuctionCount();
        int totalBids = bidDAO.getTotalBidCount();
        double revenue = invoiceDAO.getTotalRevenue();
        AdminStats stats = new AdminStats(totalUsers, activeAuctions, totalBids, revenue);
        return new Response(MessageType.ADMIN_STATS_RESPONSE, "SUCCESS", "Stats fetched", gson.toJson(stats));
    }

    private Response getUsers() {
        List<User> users = userDAO.getAllUsers();
        return new Response(MessageType.ADMIN_USERS_RESPONSE, "SUCCESS", "Users fetched", gson.toJson(users));
    }

    private Response getAuctions() {
        List<Item> items = itemDAO.getAllItems();
        return new Response(MessageType.ADMIN_AUCTIONS_RESPONSE, "SUCCESS", "Auctions fetched", gson.toJson(items));
    }

    private Response getPendingTopups() {
        List<TopupRequest> pending = walletDAO.getPendingRequests();
        return new Response(MessageType.ADMIN_PENDING_TOPUPS_RESPONSE, "SUCCESS", "Requests fetched", gson.toJson(pending));
    }

    private Response approveTopup(String requestId) {
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
    }

    private Response rejectTopup(String requestId) {
        boolean success = walletDAO.updateRequestStatus(requestId, "REJECTED");
        return new Response(MessageType.ADMIN_REJECT_TOPUP, success ? "SUCCESS" : "FAIL",
                success ? "Request rejected" : "Failed to reject request", null);
    }
}
