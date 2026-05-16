package com.auction.shared.dto;

/**
 * AdminStats DTO for dashboard statistics.
 */
public class AdminStats {
    public int totalUsers;
    public int activeAuctions;
    public int totalBids;
    public double revenue;

    public AdminStats(int totalUsers, int activeAuctions, int totalBids, double revenue) {
        this.totalUsers = totalUsers;
        this.activeAuctions = activeAuctions;
        this.totalBids = totalBids;
        this.revenue = revenue;
    }
}
