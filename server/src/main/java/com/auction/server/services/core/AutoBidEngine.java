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
        private void evaluateAutoBids() {
        boolean bidChanged;
        do {
            bidChanged = false;
            double currentHighestBid = auctionService.getCurrentHighestBid();
            String currentHighestBidder = auctionService.getCurrentHighestBidder();
            double minIncrement = auctionService.getMinIncrement();
            double requiredMinBid = currentHighestBid + minIncrement;

            AutoBidSettings bestCandidate = null;

            for (AutoBidSettings ab : autoBidders.values()) {
                if (!ab.getBidderId().equals(currentHighestBidder) && ab.isActive()) {
                    if (ab.getMaxPrice() >= requiredMinBid) {
                        if (bestCandidate == null || ab.getMaxPrice() > bestCandidate.getMaxPrice()) {
                            bestCandidate = ab;
                        }
                    }
                }
            }

            if (bestCandidate != null) {
                double step = bestCandidate.isAggressiveMode() ? bestCandidate.getBidIncrement() : minIncrement;
                double nextBid = currentHighestBid + step;

                // Cắt ngọn
                if (nextBid > bestCandidate.getMaxPrice()) {
                    nextBid = bestCandidate.getMaxPrice();
                }

                if (nextBid >= currentHighestBid + minIncrement) {
                    // Nhờ Nhạc trưởng ghi nhận lượt đánh này để đồng bộ SQLite & RAM an toàn
                    boolean success = auctionService.processAutoBid(bestCandidate.getBidderId(), nextBid);
                    if (success) bidChanged = true; 
                }
            }
        } while (bidChanged);
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