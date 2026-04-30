package com.auction.client.services;

import com.auction.shared.models.AutoBidSettings;
import com.auction.shared.models.BidTransaction;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Service to handle bidding logic locally for now.
 * Simulates MVC Controller -> Service -> Network architecture.
 */
public class BidService {

    private static BidService instance;
    private double currentBidAmount;
    private double minimumIncrement;
    private boolean isAuctionOpen = true;
    
    private final List<BidTransaction> bidHistory = new ArrayList<>();
    private final List<AutoBidSettings> autoBids = new ArrayList<>();
    
    private Consumer<Double> onPriceUpdated;
    private Consumer<BidTransaction> onNewBid;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private BidService() {
        // Dummy initialization
        this.currentBidAmount = 1240.00;
        this.minimumIncrement = 20.00;
        
        // Simulating external bids for Auto-bidding demonstration
        scheduler.scheduleAtFixedRate(this::simulateExternalBid, 15, 20, TimeUnit.SECONDS);
    }

    public static BidService getInstance() {
        if (instance == null) {
            instance = new BidService();
        }
        return instance;
    }

    public void setCallbacks(Consumer<Double> onPriceUpdated, Consumer<BidTransaction> onNewBid) {
        this.onPriceUpdated = onPriceUpdated;
        this.onNewBid = onNewBid;
    }

    public double getCurrentBidAmount() {
        return currentBidAmount;
    }

    public double getMinimumIncrement() {
        return minimumIncrement;
    }

    public void setAuctionClosed() {
        this.isAuctionOpen = false;
    }

    /**
     * Attempts to place a bid. Validates the bid.
     * @return error message if invalid, or null if successful
     */
    public String placeBid(String bidderId, String auctionId, double amount) {
        if (!isAuctionOpen) {
            return "The auction is already closed.";
        }
        
        double minBid = currentBidAmount + minimumIncrement;
        if (amount < minBid) {
            return String.format("Minimum bid is $%.2f", minBid);
        }

        // Place the bid
        processNewBid(bidderId, auctionId, amount);
        
        return null; // Success
    }
    
    /**
     * Sets up auto-bidding for a user.
     */
    public String setupAutoBid(String bidderId, String auctionId, double maxPrice, double bidIncrement) {
        if (!isAuctionOpen) {
            return "The auction is already closed.";
        }
        
        if (maxPrice <= currentBidAmount) {
            return "Max price must be higher than the current bid.";
        }
        
        // Remove existing auto-bid for this user
        autoBids.removeIf(ab -> ab.getBidderId().equals(bidderId) && ab.getAuctionId().equals(auctionId));
        
        AutoBidSettings settings = new AutoBidSettings(bidderId, auctionId, maxPrice, bidIncrement);
        autoBids.add(settings);
        
        // Trigger auto-bid evaluation immediately in case they need to take the lead
        evaluateAutoBids(auctionId);
        
        return null;
    }

    private synchronized void processNewBid(String bidderId, String auctionId, double amount) {
        if (!isAuctionOpen) return;
        
        this.currentBidAmount = amount;
        
        BidTransaction transaction = new BidTransaction(
                UUID.randomUUID().toString(),
                auctionId,
                bidderId,
                amount,
                System.currentTimeMillis()
        );
        bidHistory.add(transaction);
        
        if (onPriceUpdated != null) {
            Platform.runLater(() -> onPriceUpdated.accept(amount));
        }
        if (onNewBid != null) {
            Platform.runLater(() -> onNewBid.accept(transaction));
        }
        
        // After any new bid, evaluate auto bids
        evaluateAutoBids(auctionId);
    }
    
    private void evaluateAutoBids(String auctionId) {
        if (!isAuctionOpen) return;
        
        // Find the active auto-bids that can still bid
        List<AutoBidSettings> activeBids = new ArrayList<>();
        for (AutoBidSettings ab : autoBids) {
            if (ab.isActive() && ab.getAuctionId().equals(auctionId)) {
                activeBids.add(ab);
            }
        }
        
        if (activeBids.isEmpty()) return;
        
        // Find the auto-bid that should win
        AutoBidSettings bestAutoBid = null;
        
        // Check if the current highest bidder is an auto-bidder
        String currentHighestBidder = bidHistory.isEmpty() ? "" : bidHistory.get(bidHistory.size() - 1).getBidderId();
        
        for (AutoBidSettings ab : activeBids) {
            // If they are already winning, they don't need to bid
            if (ab.getBidderId().equals(currentHighestBidder)) {
                continue;
            }
            
            // Calculate next bid
            double nextBid = currentBidAmount + ab.getBidIncrement();
            
            // Can they afford it?
            if (nextBid <= ab.getMaxPrice()) {
                if (bestAutoBid == null || ab.getMaxPrice() > bestAutoBid.getMaxPrice()) {
                    bestAutoBid = ab;
                }
            } else {
                // Deactivate if they can't meet the next minimum increment
                ab.setActive(false);
            }
        }
        
        // Execute the best auto-bid
        if (bestAutoBid != null) {
            double nextBidAmount = currentBidAmount + bestAutoBid.getBidIncrement();
            final AutoBidSettings finalBest = bestAutoBid;
            
            // Simulate network delay for auto-bidding
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // 1 second delay
                    Platform.runLater(() -> {
                        if (isAuctionOpen && currentBidAmount < finalBest.getMaxPrice()) {
                             processNewBid(finalBest.getBidderId(), auctionId, nextBidAmount);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
    
    private void simulateExternalBid() {
        if (!isAuctionOpen) return;
        
        Platform.runLater(() -> {
            // Randomly someone else bids
            double nextBidAmount = currentBidAmount + minimumIncrement;
            processNewBid("external_competitor", "auction_1", nextBidAmount);
        });
    }
    
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
