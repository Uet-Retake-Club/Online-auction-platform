package com.auction.shared.models;

/**
 * Per-item auction state: holds live in-memory data for a single active auction.
 *
 * <p>Replaces the five flat fields that {@code AuctionService} previously used for a single
 * global session. By storing one {@code AuctionState} per item ID in a
 * {@code ConcurrentHashMap}, the server can run multiple simultaneous auctions — and any
 * product type is automatically eligible for auto-bidding the moment it is registered here.
 *
 * <p>Thread safety: all writes to mutable fields go through {@code AuctionService}'s
 * {@code synchronized} methods, so no extra synchronisation is needed here.
 */
public class AuctionState {

    private final String itemId;
    private double currentHighestBid;
    private String currentHighestBidder; // null if no bids yet
    private final double startingPrice;
    private final double minIncrement; // per-item, computed from startingPrice
    private String status; // "OPEN" | "FINISHED"

    /**
     * Constructs a new AuctionState for the given item.
     *
     * @param itemId the unique identifier of the auction item
     * @param startingPrice the starting price of the item
     * @param currentHighestBid the current highest bid (may equal startingPrice at start)
     * @param currentHighestBidder the bidder ID of the current leader, or null
     */
    public AuctionState(
            String itemId,
            double startingPrice,
            double currentHighestBid,
            String currentHighestBidder) {
        this.itemId = itemId;
        this.startingPrice = startingPrice;
        this.currentHighestBid = currentHighestBid;
        this.currentHighestBidder = currentHighestBidder;
        this.minIncrement = computeMinIncrement(startingPrice);
        this.status = "OPEN";
    }

    /**
     * Computes a sensible minimum bid increment from the item's starting price.
     *
     * <ul>
     *   <li>Items ≤ $100 → $5 increment</li>
     *   <li>Items ≤ $2,000 → $20 increment</li>
     *   <li>Items ≤ $20,000 → $100 increment</li>
     *   <li>Items &gt; $20,000 → $500 increment</li>
     * </ul>
     *
     * <p>This replaces the old hardcoded global {@code minIncrement = 20.00} and makes
     * auto-bidding sensible across all price ranges (a $50 watch and a $50,000 car both
     * get a proportional step, not the same flat $20).
     */
    public static double computeMinIncrement(double startingPrice) {
        if (startingPrice <= 100.0)     return 5.0;
        if (startingPrice <= 2_000.0)   return 20.0;
        if (startingPrice <= 20_000.0)  return 100.0;
        return 500.0;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getItemId() {
        return itemId;
    }

    public double getCurrentHighestBid() {
        return currentHighestBid;
    }

    public String getCurrentHighestBidder() {
        return currentHighestBidder;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public double getMinIncrement() {
        return minIncrement;
    }

    public String getStatus() {
        return status;
    }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setCurrentHighestBid(double currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

    public void setCurrentHighestBidder(String currentHighestBidder) {
        this.currentHighestBidder = currentHighestBidder;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "AuctionState{itemId='" + itemId + "', status='" + status
                + "', highestBid=" + currentHighestBid
                + ", highestBidder='" + currentHighestBidder + "'}";
    }
}
