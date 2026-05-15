package com.auction.server.dao;

import java.util.List;

import com.auction.shared.models.Auction;

public interface AuctionDAO {
    boolean addAuction(Auction auction);
    Auction getAuctionById(String id);
    List<Auction> getAllAuctions();
}