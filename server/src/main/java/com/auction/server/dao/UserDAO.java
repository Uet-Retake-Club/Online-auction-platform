package com.auction.server.dao;

public interface UserDAO {
    boolean registerUser(String id, String username, String password, String role);
    String authenticateUser(String username, String password);
}
