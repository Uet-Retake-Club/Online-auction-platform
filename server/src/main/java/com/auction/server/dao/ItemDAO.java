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

    List<Item> getItemsBySellerId(String sellerId);
    
    List<Item> getAllItems();
    
    int getActiveAuctionCount();

    // --- ANTI-SNIPING METHODS ---
    
    /**
     * Updates the end time of an item for Anti-sniping extensions.
     * @param itemId the ID of the item
     * @param newEndTime the new extended end time in milliseconds
     * @return true if the update was successful, false otherwise
     */
    boolean updateEndTime(String itemId, long newEndTime);

    /**
     * Updates the end time using Compare-And-Swap (CAS) to prevent lost updates
     * under concurrent bidding (Race Condition).
     * @param itemId the ID of the item
     * @param expectedEndTime the expected current end time in milliseconds
     * @param newEndTime the new extended end time in milliseconds
     * @return true if the end time matched expectedEndTime and was updated, false otherwise
     */
    boolean compareAndSetEndTime(String itemId, long expectedEndTime, long newEndTime);
}