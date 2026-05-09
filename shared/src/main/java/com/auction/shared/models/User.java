package com.auction.shared.models;

/** Represents a user in the system. */
public abstract class User extends Entity {
  protected String username;
  protected String email;
  protected String role;

  /**
   * Constructs a new User.
   *
   * @param id the unique identifier
   * @param username the username
   * @param email the email address
   * @param role the user role
   */
  public User(String id, String username, String email, String role) {
    super(id);
    this.username = username;
    this.email = email;
    this.role = role;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getRole() {
    return role;
  }
}