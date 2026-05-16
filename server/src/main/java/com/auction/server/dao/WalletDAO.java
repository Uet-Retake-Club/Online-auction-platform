package com.auction.server.dao;

import com.auction.shared.models.TopupRequest;

public interface WalletDAO {
    double getBalance(String userId);
    boolean updateBalance(String userId, double amount);
    boolean createTopupRequest(String userId, double amount);
    // For admin
    java.util.List<TopupRequest> getPendingRequests();
    java.util.List<TopupRequest> getHistory(String userId);
    boolean updateRequestStatus(String requestId, String status);
}
