package com.auction.shared.models;

/** Represents a bid transaction in the auction. */
public class BidTransaction extends Entity {
  private String itemId;
  private String bidderId;
  private double bidAmount;
  private long timestamp;
  private String bidderUsername;

  /**
   * Constructs a new BidTransaction.
   *
   * @param id the unique identifier
   * @param itemId the item being bid on
   * @param bidderId the bidder making the bid
   * @param bidAmount the amount of the bid
   * @param timestamp the time of the bid
   */
  public BidTransaction(
      String id, String itemId, String bidderId, double bidAmount, long timestamp) {
    super(id);
    this.itemId = itemId;
    this.bidderId = bidderId;
    this.bidAmount = bidAmount;
    this.timestamp = timestamp;
  }

  public String getItemId() {
    return itemId;
  }

  public String getBidderId() {
    return bidderId;
  }

  public double getBidAmount() {
    return bidAmount;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getBidderUsername() {
    return bidderUsername;
  }

  public void setBidderUsername(String bidderUsername) {
    this.bidderUsername = bidderUsername;
  }
}