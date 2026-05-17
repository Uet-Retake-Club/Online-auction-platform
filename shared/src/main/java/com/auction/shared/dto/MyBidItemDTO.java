package com.auction.shared.dto;

/**
 * Data Transfer Object representing an item with user-specific bid statistics
 * and current auction status.
 */
public class MyBidItemDTO {
    public String itemId;
    public String name;
    public String description;
    public String category;
    public double myBidAmount;
    public double currentPrice;
    public long endTime;
    public String status; // "winning", "outbid", "won", "lost", "watching"

    public MyBidItemDTO() {}

    public MyBidItemDTO(String itemId, String name, String description, String category,
                        double myBidAmount, double currentPrice, long endTime, String status) {
        this.itemId = itemId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.myBidAmount = myBidAmount;
        this.currentPrice = currentPrice;
        this.endTime = endTime;
        this.status = status;
    }
}
