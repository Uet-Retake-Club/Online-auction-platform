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
            System.out.println(" [AuctionTimer] Phiên đấu giá sẽ tự động kết thúc sau " + (delay / 1000) + " giây.");
            auctionScheduler.schedule(auctionService::endAuction, delay, TimeUnit.MILLISECONDS);
        }
    }

    public void shutdown() {
        System.out.println(" [AuctionTimer] Đang dừng đồng hồ đếm ngược...");
        auctionScheduler.shutdownNow();
    }
}