package com.auction.client.services;

import java.util.concurrent.CompletableFuture;

public class AuthService {

    public static class User {
        private final String username;
        private final String email;

        public User(String username, String email) {
            this.username = username;
            this.email = email;
        }

        public String getUsername() { return username; }
        public String getEmail() { return email; }
    }

    public CompletableFuture<User> login(String email, String password) {
        // Mock implementation
        if (email.contains("@") && password.length() >= 4) {
            return CompletableFuture.completedFuture(new User("MockUser", email));
        } else {
            return CompletableFuture.failedFuture(new RuntimeException("Invalid credentials"));
        }
    }

    public CompletableFuture<User> register(String firstName, String lastName, String username, String email, String password) {
        // Mock implementation
        return CompletableFuture.completedFuture(new User(username, email));
    }
}
