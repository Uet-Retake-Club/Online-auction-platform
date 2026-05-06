package com.auction.shared.models;

public class AutoBidSettings {
    private String bidderId;
    private String auctionId;
    private double maxPrice;
    private double bidIncrement;
    private boolean active;
    private boolean aggressiveMode;

    public AutoBidSettings(String bidderId, String auctionId, double maxPrice, double bidIncrement,boolean aggressiveMode) {
        this.bidderId = bidderId;
        this.auctionId = auctionId;
        this.maxPrice = maxPrice;
        this.bidIncrement = bidIncrement;
        this.active = true;
        this.aggressiveMode=aggressiveMode;
    }

    public String getBidderId() { return bidderId; }
    public String getAuctionId() { return auctionId; }
    
    public double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(double maxPrice) { this.maxPrice = maxPrice; }
    
    public double getBidIncrement() { return bidIncrement; }
    public void setBidIncrement(double bidIncrement) { this.bidIncrement = bidIncrement; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isAggressiveMode() {return aggressiveMode;}
}
