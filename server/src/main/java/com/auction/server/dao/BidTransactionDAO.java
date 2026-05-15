package com.auction.server.dao;

import com.auction.shared.models.BidTransaction;
import java.util.List;

public interface BidTransactionDAO {
    boolean addTransaction(BidTransaction tx);
    List<BidTransaction> getHistoryByItem(String itemId);
}