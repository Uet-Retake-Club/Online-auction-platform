package com.auction.shared.models;

/** Represents a seller user. */
public class Seller extends User {
  /**
   * Constructs a new Seller.
   *
   * @param id the unique identifier
   * @param username the username
   * @param email the email address
   */
  public Seller(String id, String username, String email) {
    super(id, username, email, "SELLER");
  }

  public Seller(String id, String username, String email, String status) {
    super(id, username, email, "SELLER", status);
  }
}