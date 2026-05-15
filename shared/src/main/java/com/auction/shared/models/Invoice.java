package com.auction.shared.models;

public class Invoice extends Entity {
    private String auctionId;
    private String itemId;
    private String bidderId;
    private String sellerId;
    private double finalPrice;
    private long timestamp;
    private String status;

    public Invoice(String id, String auctionId, String itemId, String bidderId, 
                   String sellerId, double finalPrice, long timestamp, String status) {
        super(id);
        this.auctionId = auctionId;
        this.itemId = itemId;
        this.bidderId = bidderId;
        this.sellerId = sellerId;
        this.finalPrice = finalPrice;
        this.timestamp = timestamp;
        this.status = status;
    }

    // Getters và Setters tương ứng...
    public String getAuctionId() { return auctionId; }
    public String getItemId() { return itemId; }
    public String getBidderId() { return bidderId; }
    public String getSellerId() { return sellerId; }
    public double getFinalPrice() { return finalPrice; }
    public long getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}