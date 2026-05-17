package com.auction.shared.models;

/** Represents an administrator user. */
public class Admin extends User {
  /**
   * Constructs a new Admin.
   *
   * @param id the unique identifier
   * @param username the username
   * @param email the email address
   */
  public Admin(String id, String username, String email) {
    super(id, username, email, "ADMIN");
  }

  public Admin(String id, String username, String email, String status) {
    super(id, username, email, "ADMIN", status);
  }
}