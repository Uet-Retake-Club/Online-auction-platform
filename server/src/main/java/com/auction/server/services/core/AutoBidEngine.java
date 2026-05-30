package com.auction.server.services.core;

import com.auction.server.services.AuctionService;
import com.auction.shared.models.AuctionState;
import com.auction.shared.models.AutoBidSettings;
import com.auction.shared.utils.BidIncrementPolicy;

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
    // One AtomicBoolean per itemId so items don't block each other.
    private final Map<String, AtomicBoolean> evaluationPendingByItem = new ConcurrentHashMap<>();

    // Giữ liên lạc với Nhạc trưởng để đọc giá hiện tại và chốt giá mới
    private final AuctionService auctionService;

    public AutoBidEngine(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public void addAutoBidder(AutoBidSettings settings) {
        autoBidders.put(settings.getBidderId(), settings);
        System.out.println(" [AutoBidEngine] Auto-Bid activated for: " + settings.getBidderId()
            + " | item=" + settings.getItemId()
            + " | maxPrice=$" + settings.getMaxPrice()
            + " | aggressive=" + settings.isAggressiveMode());
    }

    public void removeAutoBidder(String clientId) {
        autoBidders.remove(clientId);
        System.out.println(" [AutoBidEngine] Auto-Bid deregistered for: " + clientId);
    }

    /**
     * Submits at most one evaluation task per item at a time.
     *
     * <p>Uses a per-item AtomicBoolean CAS so that even if triggerEvaluation() is called
     * concurrently (e.g. 10 simultaneous bids on the same item), only a single
     * evaluateAutoBidsForItem() task enters the thread pool for that item.
     * Items with different IDs are evaluated independently and in parallel.
     *
     * <p>This replaces the old no-arg {@code triggerEvaluation()} which had no item context
     * and was tied to the single-auction singleton state.
     *
     * @param itemId the item that just received a new bid (or had auto-bid settings registered)
     */
    public void triggerEvaluation(String itemId) {
        // Fast-path: no auto-bidders for this item at all
        boolean anyForItem = autoBidders.values().stream()
                .anyMatch(ab -> itemId.equals(ab.getItemId()));
        if (!anyForItem) return;

        AtomicBoolean pending = evaluationPendingByItem
                .computeIfAbsent(itemId, k -> new AtomicBoolean(false));

        if (pending.compareAndSet(false, true)) {
            autoBidThreadPool.submit(() -> {
                try {
                    evaluateAutoBidsForItem(itemId);
                } finally {
                    pending.set(false); // Reset flag so next trigger is accepted
                }
            });
        } else {
            System.out.println(" [AutoBidEngine] Evaluation already queued for item "
                    + itemId + ", skipping duplicate trigger.");
        }
    }

    /**
     * Evaluates all auto-bidders registered for a specific item.
     *
     * <p>Added 'synchronized' on a per-item key to make the read-evaluate-write cycle
     * atomic for that item without blocking evaluations of other items.
     *
     * <p>By interning the itemId String and synchronizing on it, combined with
     * processAutoBid() also being synchronized on the AuctionService object, we guarantee:
     * <ul>
     *   <li>Only one thread evaluates a given item at a time</li>
     *   <li>The currentHighestBid read is always fresh (read happens inside the lock)</li>
     *   <li>No two auto-bids can be committed simultaneously for the same item</li>
     *   <li>Different items are evaluated concurrently without blocking each other</li>
     * </ul>
     *
     * @param itemId the item to evaluate auto-bidders for
     */
    private void evaluateAutoBidsForItem(String itemId) {
        // Synchronize on an interned string so different items don't block each other
        synchronized (itemId.intern()) {
            AuctionState state = auctionService.getAuctionState(itemId);
            if (state == null || "FINISHED".equals(state.getStatus())) return;

            boolean bidChanged;
            int iteration = 0;
            final int MAX_ITERATIONS = 100; // Safety guard: prevents infinite loop

            do {
                bidChanged = false;
                iteration++;

                if (iteration > MAX_ITERATIONS) {
                    System.err.println(" [AutoBidEngine] WARN: Exceeded max evaluation iterations ("
                        + MAX_ITERATIONS + ") for item " + itemId + ". Stopping.");
                    break;
                }

                // Always re-read state from AuctionService (may have changed after last bid)
                state = auctionService.getAuctionState(itemId);
                if (state == null || "FINISHED".equals(state.getStatus())) break;

                double currentHighestBid = state.getCurrentHighestBid();
                String currentHighestBidder = state.getCurrentHighestBidder();

                // Floor is recalculated from the CURRENT price, not the starting price.
                double floor = BidIncrementPolicy.calculate(currentHighestBid);
                double requiredMinBid = currentHighestBid + floor;

                AutoBidSettings bestCandidate = null;

                for (AutoBidSettings ab : autoBidders.values()) {
                    // Skip bidders that are not for this item
                    if (!itemId.equals(ab.getItemId())) continue;
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
                    // Always use the bidder's configured increment — it was already validated
                    // to be >= the policy floor at registration time (AuctionService.registerAutoBid).
                    // Aggressive mode honours the same field; the only difference is that
                    // passive mode could in future be extended to use a smaller step, but
                    // for now both modes use the same user-supplied value.
                    double step    = bestCandidate.getBidIncrement();
                    double nextBid = currentHighestBid + step;

                    // Cắt ngọn: never exceed the bidder's declared maxPrice
                    if (nextBid > bestCandidate.getMaxPrice()) {
                        nextBid = bestCandidate.getMaxPrice();
                    }

                    if (nextBid >= requiredMinBid) {
                        System.out.println(" [AutoBidEngine] Iteration #" + iteration
                            + " | Item=" + itemId
                            + " | Candidate=" + bestCandidate.getBidderId()
                            + " | nextBid=$" + nextBid
                            + " | currentHighest=$" + currentHighestBid);

                        // Nhờ Nhạc trưởng ghi nhận lượt đánh này để đồng bộ SQLite & RAM an toàn
                        boolean success = auctionService.processAutoBid(
                                bestCandidate.getBidderId(), nextBid, itemId);
                        if (success) {
                            bidChanged = true;
                            System.out.println(" [AutoBidEngine] ✓ Auto-bid accepted: $" + nextBid
                                + " by " + bestCandidate.getBidderId()
                                + " for item " + itemId);
                        } else {
                            // Bid rejected (insufficient funds or auction ended).
                            // Deactivate to prevent endless retry loops for this bidder.
                            System.err.println(" [AutoBidEngine] ✗ Auto-bid rejected for "
                                + bestCandidate.getBidderId()
                                + " on item " + itemId
                                + " — insufficient funds or auction ended. Deactivating auto-bid.");
                            bestCandidate.setActive(false);
                        }
                    }
                }
            } while (bidChanged);

            System.out.println(" [AutoBidEngine] Evaluation for item " + itemId
                    + " complete after " + iteration + " iteration(s).");
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
            Thread.currentThread().interrupt(); // Restore interrupted status (best practice)
        }
    }
}