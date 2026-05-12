package com.auction.server.dao;

import com.auction.shared.models.Item;

public interface ItemDAO {
    
    Item getItemById(String id);
    
    boolean addItem(Item item);
    
    boolean updateCurrentPrice(String itemId, double newPrice, String bidderId);

    //Cập nhật trạng thái (OPEN, RUNNING, FINISHED, PAID, CANCELED)
    boolean updateStatus(String itemId, String status);
}