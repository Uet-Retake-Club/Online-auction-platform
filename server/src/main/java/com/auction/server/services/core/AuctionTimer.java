package com.auction.server.services.core;

import com.auction.server.services.AuctionService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionTimer {
    private final ScheduledExecutorService auctionScheduler = Executors.newScheduledThreadPool(1);
    private final AuctionService auctionService;

    public AuctionTimer(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public void scheduleAuctionEnd(long endTimeInMillis) {
        long delay = endTimeInMillis - System.currentTimeMillis();
        
        if (delay <= 0) {
            auctionService.endAuction();
        } else {
            System.out.println(" [AuctionTimer] Auction will end automatically in " + (delay / 1000) + " seconds.");
            auctionScheduler.schedule(auctionService::endAuction, delay, TimeUnit.MILLISECONDS);
        }
    }

    public void shutdown() {
        System.out.println(" [AuctionTimer] Stopping countdown timer...");
        auctionScheduler.shutdownNow();
    }
}