package com.auction.server.dao;

import java.util.List;

import com.auction.shared.models.Item;

public interface ItemDAO {

    Item getItemById(String id);

    Item getFirstOpenItem();

    boolean addItem(Item item);

    boolean updateCurrentPrice(String itemId, double newPrice, String bidderId);

    boolean updateStatus(String itemId, String status);

    boolean resetItemForReauction(String itemId);

    boolean updateEndTime(String itemId, long newEndTime);

    /**
     * Atomically update the end_time only if the current end_time equals the expected value.
     * Returns true if the update succeeded (rows affected == 1).
     */
    boolean compareAndSetEndTime(String itemId, long expectedEndTime, long newEndTime);

    List<Item> getItemsBySellerId(String sellerId);
    List<Item> getAllItems();
    int getActiveAuctionCount();
}
