package com.auction.server.dao;

import com.auction.shared.models.User;

public interface UserDAO {
    User getUserById(String id);
    User getUserByUsername(String username);
    boolean addUser(User user, String password);
}