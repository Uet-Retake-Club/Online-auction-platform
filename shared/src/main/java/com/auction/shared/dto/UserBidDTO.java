package com.auction.shared.dto;

public class UserBidDTO {
    private String itemId;
    private String name;
    private String category;
    private String description;
    private double startingPrice;
    private double currentHighestBid;
    private String highestBidderId;
    private long endTime;
    private String status;
    private double myHighestBid;
    private boolean isWatchlisted;

    public UserBidDTO() {
    }

    public UserBidDTO(String itemId, String name, String category, String description,
                      double startingPrice, double currentHighestBid, String highestBidderId,
                      long endTime, String status, double myHighestBid, boolean isWatchlisted) {
        this.itemId = itemId;
        this.name = name;
        this.category = category;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentHighestBid = currentHighestBid;
        this.highestBidderId = highestBidderId;
        this.endTime = endTime;
        this.status = status;
        this.myHighestBid = myHighestBid;
        this.isWatchlisted = isWatchlisted;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    public String getHighestBidderId() { return highestBidderId; }
    public void setHighestBidderId(String highestBidderId) { this.highestBidderId = highestBidderId; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getMyHighestBid() { return myHighestBid; }
    public void setMyHighestBid(double myHighestBid) { this.myHighestBid = myHighestBid; }

    public boolean isWatchlisted() { return isWatchlisted; }
    public void setWatchlisted(boolean watchlisted) { isWatchlisted = watchlisted; }
}
