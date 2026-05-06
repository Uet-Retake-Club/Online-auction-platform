package com.auction.server.dao;

import com.auction.shared.models.Item;

public interface ItemDAO {
    
    Item getItemById(int id);
    
    boolean addItem(Item item);
    
    boolean updateCurrentPrice(int itemId, double newPrice);
}