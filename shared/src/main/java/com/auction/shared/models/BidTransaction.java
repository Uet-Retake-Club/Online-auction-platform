package com.auction.shared.models;

public class BidTransaction extends Entity {
    private String itemId;
    private String bidderId;
    private double bidAmount;
    private long timestamp;

    public BidTransaction(String id, String itemId, String bidderId, double bidAmount, long timestamp) {
        super(id);
        this.itemId = itemId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.timestamp = timestamp;
    }

    public String getItemId() { return itemId; }
    public String getBidderId() { return bidderId; }
    public double getBidAmount() { return bidAmount; }
    public long getTimestamp() { return timestamp; }
}