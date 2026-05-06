package com.auction.shared.models;

public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected ItemCategory category;
    protected double startingPrice;
    protected double currentHighestBid;
    protected int highestBidderId;
    protected long startTime;
    protected long endTime;
    protected int sellerId;
    protected String status; 


    public Item(int id, String name, String description, ItemCategory category, double startingPrice, long startTime, long endTime) {
        super(id);
        this.name = name;
        this.description = description;
        this.category = category;
        this.startingPrice = startingPrice;
        this.currentHighestBid = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = "OPEN";
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    public int getHighestBidderId() { return highestBidderId; }
    public void setHighestBidderId(int highestBidderId) { this.highestBidderId = highestBidderId; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public ItemCategory getCategory() { return category; }
    public void setCategory(ItemCategory category) { this.category = category; }

    public int getSellerId() {return sellerId;}
    public void setSellerId(int sellerId) {this.sellerId = sellerId;}

    public abstract void printInfo();
}