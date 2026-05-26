package com.auction.server.services.core;

import com.auction.server.services.AuctionService;
import com.auction.shared.models.AutoBidSettings;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AutoBidEngine {
    private final ExecutorService autoBidThreadPool = Executors.newFixedThreadPool(10);
    private final Map<String, AutoBidSettings> autoBidders = new ConcurrentHashMap<>();
    
    // Giữ liên lạc với Nhạc trưởng để đọc giá hiện tại và chốt giá mới
    private final AuctionService auctionService;

    public AutoBidEngine(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public void addAutoBidder(AutoBidSettings settings) {
        autoBidders.put(settings.getBidderId(), settings);
        System.out.println(" [AutoBidEngine] Auto-Bid activated successfully for: " + settings.getBidderId());
    }

    public void removeAutoBidder(String clientId) {
        autoBidders.remove(clientId);
    }

    public void triggerEvaluation() {
        autoBidThreadPool.submit(this::evaluateAutoBids);
    }

    /**
     * UPDATED LOGIC:
     * Uses synchronized to prevent Race Conditions when multiple threads execute.
     * Simulates the entire price increment process in RAM to protect SQLite and Socket bandwidth.
     */
    private synchronized void evaluateAutoBids() {
        // 1. Fetch the current auction state from AuctionService's RAM
        double simHighestBid = auctionService.getCurrentHighestBid();
        String simHighestBidder = auctionService.getCurrentHighestBidder();
        double minIncrement = auctionService.getMinIncrement();

        String finalBidderId = null;
        double finalBidAmount = 0.0;
        boolean bidChanged;

        // 2. IN-MEMORY SIMULATION: Calculate price jumps using local variables
        do {
            bidChanged = false;
            double requiredMinBid = simHighestBid + minIncrement;
            AutoBidSettings bestCandidate = null;

            // Find the robot with the highest max price that is eligible to bid
            for (AutoBidSettings ab : autoBidders.values()) {
                if (!ab.getBidderId().equals(simHighestBidder) && ab.isActive()) {
                    if (ab.getMaxPrice() >= requiredMinBid) {
                        if (bestCandidate == null || ab.getMaxPrice() > bestCandidate.getMaxPrice()) {
                            bestCandidate = ab;
                        }
                    }
                }
            }

            // If a valid robot is found, proceed with the simulated bid increment
            if (bestCandidate != null) {
                double step = bestCandidate.isAggressiveMode() ? bestCandidate.getBidIncrement() : minIncrement;
                double nextBid = simHighestBid + step;

                // Cap the bid if it exceeds the robot's pre-set maximum limit
                if (nextBid > bestCandidate.getMaxPrice()) {
                    nextBid = bestCandidate.getMaxPrice();
                }

                if (nextBid >= simHighestBid + minIncrement) {
                    // Update the internal simulated state for the next loop iteration
                    simHighestBidder = bestCandidate.getBidderId();
                    simHighestBid = nextBid;

                    // Record the information of the last valid bid
                    finalBidderId = simHighestBidder;
                    finalBidAmount = simHighestBid;
                    bidChanged = true; 
                }
            }
        } while (bidChanged); // Loop runs extremely fast on CPU (No I/O bottlenecks)

        // 3. FINALIZE RESULT: Push to SQLite and send Socket broadcast ONLY ONCE
        if (finalBidderId != null && finalBidAmount > 0.0) {
            System.out.println(" [AutoBidEngine] Simulation ended. Final candidate of this round: " 
                    + finalBidderId + " with price: $" + finalBidAmount);
            
            // Call the Manager to process the final result exactly once for the entire auto-bid session
            auctionService.processAutoBid(finalBidderId, finalBidAmount);
        }
    }

    // Đã trả lại y nguyên logic đóng an toàn của bạn
    public void shutdown() {
        System.out.println(" [AutoBidEngine] Stopping Robot system...");
        autoBidThreadPool.shutdown();
        try {
            if (!autoBidThreadPool.awaitTermination(3, TimeUnit.SECONDS)) {
                autoBidThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            autoBidThreadPool.shutdownNow();
        }
    }
}