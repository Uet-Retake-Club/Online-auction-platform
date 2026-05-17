package com.auction.shared.models;

import java.util.ArrayList;
import java.util.List;

/** Represents an auction event. */
public class Auction extends Entity {
  private Item item;
  private final Seller seller;
  private final List<BidTransaction> bidHistory;

  /**
   * Constructs a new Auction.
   *
   * @param id the unique identifier
   * @param item the item being auctioned
   * @param seller the seller of the item
   */
  public Auction(String id, Item item, Seller seller) {
    super(id);
    this.item = item;
    this.seller = seller;
    this.bidHistory = new ArrayList<>();
  }

  public Item getItem() {
    return item;
  }

  public void setItem(Item item) {
    this.item = item;
  }

  public Seller getSeller() {
    return seller;
  }

  public List<BidTransaction> getBidHistory() {
    return bidHistory;
  }

  public void addBid(BidTransaction transaction) {
    this.bidHistory.add(transaction);
  }
}