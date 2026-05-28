package com.auction.server.services.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.auction.server.services.AuctionService;

public class AuctionTimer {
    // Use a ThreadFactory to make scheduler threads daemon so the JVM can exit cleanly on shutdown
    private final ScheduledExecutorService auctionScheduler = Executors.newScheduledThreadPool(1, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("AuctionTimer-Scheduler");
        return thread;
    });
    
    private final AuctionService auctionService;
    
    private ScheduledFuture<?> currentEndTask = null;
    // Guard: only a task whose end time matches `currentEndTime` may end the auction
    private long currentEndTime = -1;

    public AuctionTimer(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public synchronized void scheduleAuctionEnd(long endTimeInMillis) {
        // 1. Cancel previous task if it hasn't run yet
        if (currentEndTask != null && !currentEndTask.isDone()) {
            currentEndTask.cancel(false);
        }

        // 2. Update the latest valid end time
        this.currentEndTime = endTimeInMillis;
        long delay = endTimeInMillis - System.currentTimeMillis();
        
        if (delay <= 0) {
            // If the provided time is already past, clear state and end the auction immediately
            clearState();
            auctionService.endAuction();
        } else {
            System.out.println(" [AuctionTimer] Scheduled auction end in " + (delay / 1000) + " seconds.");

            // Schedule and save the new background task
            currentEndTask = auctionScheduler.schedule(() -> {
                boolean shouldExecute = false;
                
                // Synchronized check to verify whether this task is stale
                synchronized (this) {
                    if (endTimeInMillis == currentEndTime) {
                        shouldExecute = true;
                        // Dọn dẹp trạng thái ngay khi xác nhận thực thi thành công
                        // Clear state immediately when confirming execution
                        clearState();
                    }
                }
                
                // Execute auction end outside the Timer's synchronized block to avoid deadlocks with the Service
                if (shouldExecute) {
                    System.out.println(" [AuctionTimer] Time reached. Triggering auction end...");
                    auctionService.endAuction();
                } else {
                    System.out.println(" [AuctionTimer] Detected stale countdown task; no-op to prevent duplicate end calls.");
                }
            }, delay, TimeUnit.MILLISECONDS);
        }
    }

    // Utility to clear scheduler state
    private void clearState() {
        this.currentEndTask = null;
        this.currentEndTime = -1;
    }

    public synchronized void shutdown() {
        System.out.println(" [AuctionTimer] Stopping countdown timer...");
        clearState();
        auctionScheduler.shutdownNow();
    }
}