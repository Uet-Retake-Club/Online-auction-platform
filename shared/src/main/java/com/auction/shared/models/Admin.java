package com.auction.shared.models;

public class Admin extends User {
    public Admin(String id, String username, String email) {
        super(id, username, email, "ADMIN");
    }
}