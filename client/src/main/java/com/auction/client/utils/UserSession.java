package com.auction.client.utils;

public final class UserSession {

    private static UserSession instance;

    private String firstName = "";
    private String lastName = "";
    private String username = "";
    private String email = "";
    private String role = "GUEST";

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void signIn(String firstName, String lastName, String username, String email, String role) {
        this.firstName = firstName == null ? "" : firstName.trim();
        this.lastName = lastName == null ? "" : lastName.trim();
        this.username = username == null ? "" : username.trim();
        this.email = email == null ? "" : email.trim();
        this.role = role == null ? "GUEST" : role.trim().toUpperCase();
    }

    public void clear() {
        signIn("", "", "", "", "GUEST");
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUsername() {
        if (!username.isBlank()) {
            return username;
        }
        if (!email.isBlank()) {
            int at = email.indexOf('@');
            return at > 0 ? email.substring(0, at) : email;
        }
        return "guest";
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public boolean isLoggedIn() {
        return !username.isBlank() || !email.isBlank();
    }

    public String getDisplayName() {
        if (!firstName.isBlank() || !lastName.isBlank()) {
            return (firstName + " " + lastName).trim();
        }
        if (!username.isBlank()) {
            return username;
        }
        if (!email.isBlank()) {
            return email;
        }
        return "Guest";
    }

    public String getInitials() {
        if (!firstName.isBlank() || !lastName.isBlank()) {
            StringBuilder initials = new StringBuilder();
            if (!firstName.isBlank()) {
                initials.append(Character.toUpperCase(firstName.charAt(0)));
            }
            if (!lastName.isBlank()) {
                initials.append(Character.toUpperCase(lastName.charAt(0)));
            }
            return initials.length() > 0 ? initials.toString() : "G";
        }
        if (!username.isBlank()) {
            return username.length() >= 2
                ? username.substring(0, 2).toUpperCase()
                : username.toUpperCase();
        }
        if (!email.isBlank()) {
            return email.substring(0, 1).toUpperCase();
        }
        return "G";
    }
}

