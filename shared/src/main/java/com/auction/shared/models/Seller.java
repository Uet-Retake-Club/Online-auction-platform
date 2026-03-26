package com.auction.shared.models;

public class Seller extends User {
    public Seller(String id, String username, String email) {
        super(id, username, email, "SELLER");
    }
}