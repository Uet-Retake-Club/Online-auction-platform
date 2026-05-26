package com.auction.server.services.core;

import com.auction.server.services.AuctionService;
import com.auction.shared.models.AutoBidSettings;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoBidEngine {
    private final ExecutorService autoBidThreadPool = Executors.newFixedThreadPool(10);
    private final Map<String, AutoBidSettings> autoBidders = new ConcurrentHashMap<>();

    // FIX BUG #2: Prevents duplicate concurrent evaluation tasks from flooding the thread pool.
    // Uses AtomicBoolean CAS so only one evaluation task is ever queued at a time.
    private final AtomicBoolean evaluationPending = new AtomicBoolean(false);

    // Giữ liên lạc với Nhạc trưởng để đọc giá hiện tại và chốt giá mới
    private final AuctionService auctionService;

    public AutoBidEngine(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public void addAutoBidder(AutoBidSettings settings) {
        autoBidders.put(settings.getBidderId(), settings);
        System.out.println(" [AutoBidEngine] Auto-Bid activated for: " + settings.getBidderId()
            + " | maxPrice=$" + settings.getMaxPrice()
            + " | aggressive=" + settings.isAggressiveMode());
    }

    public void removeAutoBidder(String clientId) {
        autoBidders.remove(clientId);
        System.out.println(" [AutoBidEngine] Auto-Bid deregistered for: " + clientId);
    }

    /**
     * FIX BUG #2: Submits at most one evaluation task at a time.
     *
     * <p>Uses AtomicBoolean CAS (compare-and-set) to guarantee that even if
     * triggerEvaluation() is called 10 times concurrently (e.g., 10 simultaneous bids),
     * only a single evaluateAutoBids() task enters the thread pool.
     * The flag is reset in the finally block after the task completes, so the next
     * trigger after the evaluation finishes will be accepted normally.
     */
    public void triggerEvaluation() {
        if (autoBidders.isEmpty()) return; // Fast-path: no auto-bidders registered
        if (evaluationPending.compareAndSet(false, true)) {
            autoBidThreadPool.submit(() -> {
                try {
                    evaluateAutoBids();
                } finally {
                    evaluationPending.set(false); // Reset flag so next trigger is accepted
                }
            });
        } else {
            System.out.println(" [AutoBidEngine] Evaluation already queued, skipping duplicate trigger.");
        }
    }

    /**
     * FIX BUG #1: Added 'synchronized' to make the entire read-evaluate-write cycle atomic.
     *
     * <p>The original code's comment claimed it "uses synchronized to prevent Race Conditions"
     * but the method had NO synchronized keyword — allowing multiple threads to read the same
     * stale currentHighestBid and produce duplicate or invalid bids.
     *
     * <p>By synchronizing this method, combined with processAutoBid() also being synchronized
     * on the AuctionService object, we guarantee:
     * <ul>
     *   <li>Only one thread evaluates at a time</li>
     *   <li>The currentHighestBid read is always fresh (read happens inside the lock)</li>
     *   <li>No two auto-bids can be committed simultaneously for the same item</li>
     * </ul>
     */
    private synchronized void evaluateAutoBids() {
        if (autoBidders.isEmpty()) return;

        boolean bidChanged;
        int iteration = 0;
        final int MAX_ITERATIONS = 100; // Safety guard: prevents infinite loop if logic errors occur

        do {
            bidChanged = false;
            iteration++;

            if (iteration > MAX_ITERATIONS) {
                System.err.println(" [AutoBidEngine] WARN: Exceeded max evaluation iterations ("
                    + MAX_ITERATIONS + "). Stopping to prevent CPU runaway.");
                break;
            }

            double currentHighestBid = auctionService.getCurrentHighestBid();
            String currentHighestBidder = auctionService.getCurrentHighestBidder();
            double minIncrement = auctionService.getMinIncrement();
            double requiredMinBid = currentHighestBid + minIncrement;

            AutoBidSettings bestCandidate = null;

            for (AutoBidSettings ab : autoBidders.values()) {
                // Skip bidders who are inactive or already leading
                if (!ab.isActive()) continue;
                if (ab.getBidderId().equals(currentHighestBidder)) continue;

                if (ab.getMaxPrice() >= requiredMinBid) {
                    // Prefer the bidder with the highest ceiling (they will win in the end)
                    if (bestCandidate == null || ab.getMaxPrice() > bestCandidate.getMaxPrice()) {
                        bestCandidate = ab;
                    }
                }
            }

            if (bestCandidate != null) {
                double step = bestCandidate.isAggressiveMode()
                    ? bestCandidate.getBidIncrement()
                    : minIncrement;
                double nextBid = currentHighestBid + step;

                // Cắt ngọn: never exceed the bidder's declared maxPrice
                if (nextBid > bestCandidate.getMaxPrice()) {
                    nextBid = bestCandidate.getMaxPrice();
                }

                if (nextBid >= requiredMinBid) {
                    System.out.println(" [AutoBidEngine] Iteration #" + iteration
                        + " | Candidate=" + bestCandidate.getBidderId()
                        + " | nextBid=$" + nextBid
                        + " | currentHighest=$" + currentHighestBid);

                    // Nhờ Nhạc trưởng ghi nhận lượt đánh này để đồng bộ SQLite & RAM an toàn
                    boolean success = auctionService.processAutoBid(bestCandidate.getBidderId(), nextBid);
                    if (success) {
                        bidChanged = true;
                        System.out.println(" [AutoBidEngine] ✓ Auto-bid accepted: $" + nextBid
                            + " by " + bestCandidate.getBidderId());
                    } else {
                        // Bid rejected (insufficient funds or auction ended).
                        // Deactivate to prevent endless retry loops for this bidder.
                        System.err.println(" [AutoBidEngine] ✗ Auto-bid rejected for "
                            + bestCandidate.getBidderId()
                            + " — insufficient funds or auction ended. Deactivating auto-bid.");
                        bestCandidate.setActive(false);
                    }
                }
            }
        } while (bidChanged);

        System.out.println(" [AutoBidEngine] Evaluation complete after " + iteration + " iteration(s).");
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
            Thread.currentThread().interrupt(); // Restore interrupted status (best practice)
        }
    }
}