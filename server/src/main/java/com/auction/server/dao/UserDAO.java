package com.auction.server.dao;

public interface UserDAO {
    boolean registerUser(String id, String username, String email, String password, String role);
    String authenticateUser(String emailOrUsername, String password);
}
