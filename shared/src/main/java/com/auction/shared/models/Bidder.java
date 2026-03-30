package com.auction.shared.models;

public class Bidder extends User {
    public Bidder(String id, String username, String email) {
        super(id, username, email, "BIDDER");
    }
}