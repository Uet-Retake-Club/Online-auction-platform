package com.auction.server.dao;

public interface WalletDAO {
    double getBalance(String userId);
    boolean updateBalance(String userId, double amount);
    boolean createTopupRequest(String userId, double amount);
    // For admin
    java.util.List<TopupRequest> getPendingRequests();
    boolean updateRequestStatus(String requestId, String status);

    class TopupRequest {
        public String id;
        public String userId;
        public double amount;
        public String status;
        public long timestamp;

        public TopupRequest(String id, String userId, double amount, String status, long timestamp) {
            this.id = id;
            this.userId = userId;
            this.amount = amount;
            this.status = status;
            this.timestamp = timestamp;
        }
    }
}
