package com.auction.shared.models;

public class BidTransaction extends Entity {
    private final int itemId;
    private final int bidderId;
    private final double bidAmount;
    private final long timestamp;

    public BidTransaction(int id, int itemId, int bidderId, double bidAmount, long timestamp) {
        super(id);
        this.itemId = itemId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.timestamp = timestamp;
    }

    public int getItemId() { return itemId; }
    public int getBidderId() { return bidderId; }
    public double getBidAmount() { return bidAmount; }
    public long getTimestamp() { return timestamp; }
}