package com.auction.shared.models;

/** Base class for auction items. */
public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected ItemCategory category;
    protected double startingPrice;
    protected double currentHighestBid;
    protected String highestBidderId;
    protected long startTime;
    protected long endTime;
    protected String sellerId;
    protected String status; 


    public Item(String id, String name, String description, ItemCategory category, double startingPrice, long startTime, long endTime) {
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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

<<<<<<< HEAD
    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }
=======
  public String getDescription() {
    return description;
  }
>>>>>>> 550407a625752cc11c7dce67e6e75152fca49ea3

  public void setDescription(String description) {
    this.description = description;
  }

  public double getStartingPrice() {
    return startingPrice;
  }

  public double getCurrentHighestBid() {
    return currentHighestBid;
  }

  public void setCurrentHighestBid(double currentHighestBid) {
    this.currentHighestBid = currentHighestBid;
  }

  public String getHighestBidderId() {
    return highestBidderId;
  }

<<<<<<< HEAD
    public ItemCategory getCategory() { return category; }
    public void setCategory(ItemCategory category) { this.category = category; }

    public String getSellerId() {return sellerId;}
    public void setSellerId(String sellerId) {this.sellerId = sellerId;}

    public abstract void printInfo();
=======
  public void setHighestBidderId(String highestBidderId) {
    this.highestBidderId = highestBidderId;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public abstract void printInfo();
>>>>>>> 550407a625752cc11c7dce67e6e75152fca49ea3
}