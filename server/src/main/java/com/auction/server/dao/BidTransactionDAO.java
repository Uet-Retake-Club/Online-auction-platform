package com.auction.server.dao;

import com.auction.shared.models.BidTransaction;
import java.util.List;

public interface BidTransactionDAO {
    boolean addTransaction(BidTransaction tx);
    List<BidTransaction> getHistoryByItem(String itemId);
    List<BidTransaction> getAllTransactions();
    int getTotalBidCount();
    double getMaxBidAmount(String userId, String itemId);
    List<String> getBiddersForItem(String itemId);
    List<String> getBiddedItemIds(String userId);
    BidTransaction getSecondHighestBid(String itemId);
}