package com.auction.server.dao;

import com.auction.shared.models.Item;
import java.util.List;

public interface WatchlistDAO {
    boolean addToWatchlist(String userId, String itemId);
    boolean removeFromWatchlist(String userId, String itemId);
    List<Item> getWatchlist(String userId);
    boolean isInWatchlist(String userId, String itemId);
}
