package com.auction.server.services.core;

import com.auction.server.services.AuctionService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionTimer {
    private final ScheduledExecutorService auctionScheduler = Executors.newScheduledThreadPool(4);
    private final AuctionService auctionService;

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
        long delay = endTimeInMillis - System.currentTimeMillis();

        if (delay <= 0) {
            auctionService.endAuction(itemId);
        } else {
            System.out.println(" [AuctionTimer] Item " + itemId
                    + " will end automatically in " + (delay / 1000) + " seconds.");
            auctionScheduler.schedule(
                    () -> auctionService.endAuction(itemId), delay, TimeUnit.MILLISECONDS);
        }
    }

    public void shutdown() {
        System.out.println(" [AuctionTimer] Stopping countdown timer...");
        auctionScheduler.shutdownNow();
    }
}