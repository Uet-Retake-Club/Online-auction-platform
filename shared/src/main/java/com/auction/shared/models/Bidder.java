package com.auction.shared.models;

/** Represents a bidder user. */
public class Bidder extends User {
  /**
   * Constructs a new Bidder.
   *
   * @param id the unique identifier
   * @param username the username
   * @param email the email address
   */
  public Bidder(String id, String username, String email) {
    super(id, username, email, "BIDDER");
  }

  public Bidder(String id, String username, String email, String status) {
    super(id, username, email, "BIDDER", status);
  }
}