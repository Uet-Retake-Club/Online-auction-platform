package com.auction.server.services.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.auction.server.services.AuctionService;

public class AuctionTimer {
    private final ScheduledExecutorService auctionScheduler = Executors.newScheduledThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("AuctionTimer-Scheduler");
        return thread;
    });
    private final AuctionService auctionService;

    // Track tasks and end times per item for multi-auction support
    private final Map<String, ScheduledFuture<?>> currentTasks = new ConcurrentHashMap<>();
    private final Map<String, Long> currentEndTimes = new ConcurrentHashMap<>();

    public AuctionTimer(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    /**
     * Schedules the end of the auction for a specific item.
     *
     * <p>Replaces the old no-arg {@code scheduleAuctionEnd(long)} which called the global
     * {@code endAuction()} without knowing which item to close. Now each item has its own
     * scheduled task so multiple auctions can run and end independently.
     *
     * @param itemId the item whose auction will be closed at {@code endTimeInMillis}
     * @param endTimeInMillis absolute epoch timestamp (ms) when the auction ends
     */
    public void scheduleAuctionEnd(String itemId, long endTimeInMillis) {
        // Cancel old task for this specific item if it exists
        ScheduledFuture<?> oldTask = currentTasks.get(itemId);
        if (oldTask != null && !oldTask.isDone()) {
            oldTask.cancel(false);
        }

        // Update with the newest valid end time
        currentEndTimes.put(itemId, endTimeInMillis);
        long delay = endTimeInMillis - System.currentTimeMillis();

        if (delay <= 0) {
            clearState(itemId);
            auctionService.endAuction(itemId);
        } else {
            System.out.println(" [AuctionTimer] Item " + itemId
                    + " will end automatically in " + (delay / 1000) + " seconds.");
            
            ScheduledFuture<?> newTask = auctionScheduler.schedule(() -> {
                boolean shouldExecute = false;
                Long expectedTime = currentEndTimes.get(itemId);
                
                // Double-check to prevent stale tasks from closing an extended auction
                if (expectedTime != null && expectedTime == endTimeInMillis) {
                    shouldExecute = true;
                    clearState(itemId);
                }
                
                if (shouldExecute) {
                    System.out.println(" [AuctionTimer] Reached end time. Triggering endAuction for item " + itemId);
                    auctionService.endAuction(itemId);
                } else {
                    System.out.println(" [AuctionTimer] Stale task no-op for item " + itemId + ". Prevented premature end!");
                }
            }, delay, TimeUnit.MILLISECONDS);

            currentTasks.put(itemId, newTask);
        }
    }

    private void clearState(String itemId) {
        currentTasks.remove(itemId);
        currentEndTimes.remove(itemId);
    }

    public void shutdown() {
        System.out.println(" [AuctionTimer] Stopping countdown timer...");
        currentTasks.clear();
        currentEndTimes.clear();
        auctionScheduler.shutdownNow();
    }
}