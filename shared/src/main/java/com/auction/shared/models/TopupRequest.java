package com.auction.shared.models;

public class TopupRequest {
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
