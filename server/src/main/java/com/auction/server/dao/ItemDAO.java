package com.auction.server.dao;

import com.auction.shared.models.Item;
import java.util.List;

public interface ItemDAO {

    Item getItemById(String id);

    Item getFirstOpenItem();

    boolean addItem(Item item);

    boolean updateCurrentPrice(String itemId, double newPrice, String bidderId);

    boolean updateStatus(String itemId, String status);

    List<Item> getItemsBySellerId(String sellerId);
    List<Item> getAllItems();
}