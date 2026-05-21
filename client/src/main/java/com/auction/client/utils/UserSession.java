package com.auction.client.utils;

/**
 * UserSession stores the currently logged-in user's state.
 *
 * <p>All controllers read from this class instead of hard-coding display values.
 *
 * <p>Usage:
 * <pre>
 *   // After successful login:
 *   UserSession.getInstance().signIn(firstName, lastName, username, email, role);
 *
 *   // In any controller:
 *   String initials  = UserSession.getInstance().getInitials();
 *   String firstName = UserSession.getInstance().getFirstName();
 *   boolean isAdmin  = UserSession.getInstance().isAdmin();
 * </pre>
 */
public final class UserSession {

  /**
   * The single instance.
   */
  private static UserSession instance;

  /**
   * Database ID of the logged-in user.
   */
  private String userId;

  /**
   * First name of the logged-in user.
   */
  private String firstName;

  /**
   * Last name of the logged-in user.
   */
  private String lastName;

  /**
   * Username / display handle.
   */
  private String username;

  /**
   * Email address.
   */
  private String email;

  /**
   * Role: BIDDER, SELLER, or ADMIN.
   */
  private String role;

  /** Wallet balance of the logged-in user. */
  private double walletBalance = 0.0;

  /** Title of the currently selected auction. */
  private String selectedAuctionTitle = "";

  /** Category of the currently selected auction. */
  private String selectedAuctionCategory = "";

  /** DB item ID of the currently selected auction. */
  private String selectedItemId = "ITEM-001";

  /** Starting/current price shown on the card. */
  private double selectedItemPrice = 0.0;

  /** Description of the currently selected auction item. */
  private String selectedItemDescription = "";

  /** Filter to apply when navigating to My Bids view (e.g. "watching"). */
  private String pendingMyBidsFilter = "";

  public String getPendingMyBidsFilter() {
    return pendingMyBidsFilter;
  }

  public void setPendingMyBidsFilter(final String filter) {
    this.pendingMyBidsFilter = filter;
  }

  public String getSelectedAuctionTitle() {
    return selectedAuctionTitle;
  }

  public void setSelectedAuctionTitle(final String title) {
    this.selectedAuctionTitle = title;
  }

  public String getSelectedAuctionCategory() {
    return selectedAuctionCategory;
  }

  public void setSelectedAuctionCategory(final String category) {
    this.selectedAuctionCategory = category;
  }

  public String getSelectedItemId() {
    return selectedItemId;
  }

  public void setSelectedItemId(final String itemId) {
    this.selectedItemId = itemId;
  }

  public double getSelectedItemPrice() {
    return selectedItemPrice;
  }

  public void setSelectedItemPrice(final double price) {
    this.selectedItemPrice = price;
  }

  public String getSelectedItemDescription() {
    return selectedItemDescription;
  }

  public void setSelectedItemDescription(final String description) {
    this.selectedItemDescription = description;
  }

  /**
   * Private constructor — singleton.
   */
  private UserSession() {
  }

  /**
   * Returns the singleton instance, creating it if necessary.
   *
   * @return the UserSession instance
   */
  public static synchronized UserSession getInstance() {
    if (instance == null) {
      instance = new UserSession();
    }
    return instance;
  }

  // ── Sign in / out ─────────────────────────────────────────
  /**
   * Populates the session after a successful login or registration. Called
   * from LoginController / SignupController after backend confirms.
   *
   * @param userFirstName the user's first name
   * @param userLastName the user's last name
   * @param userUsername the username / handle
   * @param userEmail the email address
   * @param userRole BIDDER, SELLER, or ADMIN
   */
  public void signIn(
      final String userId,
      final String userFirstName,
      final String userLastName,
      final String userUsername,
      final String userEmail,
      final String userRole) {
    this.userId = userId;
    this.firstName = userFirstName;
    this.lastName = userLastName;
    this.username = userUsername;
    this.email = userEmail;
    this.role = userRole;
  }

  /**
   * Clears all session data (logout). Call this before navigating back to
   * LoginView.
   */
  public void clear() {
    userId = null;
    firstName = null;
    lastName = null;
    username = null;
    email = null;
    role = null;
    walletBalance = 0.0;
  }

  // ── Getters ───────────────────────────────────────────────
  /**
   * Returns the user's database ID.
   *
   * @return user ID, or empty string if not set
   */
  public String getUserId() {
    return userId != null ? userId : "";
  }

  /**
   * Returns the user's first name.
   *
   * @return first name, or empty string if not set
   */
  public String getFirstName() {
    return firstName != null ? firstName : "";
  }

  /**
   * Returns the user's last name.
   *
   * @return last name, or empty string if not set
   */
  public String getLastName() {
    return lastName != null ? lastName : "";
  }

  /**
   * Returns the username.
   *
   * @return username, or empty string if not set
   */
  public String getUsername() {
    return username != null ? username : "";
  }

  /**
   * Returns the email address.
   *
   * @return email, or empty string if not set
   */
  public String getEmail() {
    return email != null ? email : "";
  }

  /**
   * Returns the role string.
   *
   * @return role (BIDDER / SELLER / ADMIN), or empty string if not set
   */
  public String getRole() {
    return role != null ? role : "";
  }

  // ── Setters (for profile update) ──────────────────────────
  /**
   * Updates the first name after profile save.
   *
   * @param value new first name
   */
  public void setFirstName(final String value) {
    this.firstName = value;
  }

  /**
   * Updates the last name after profile save.
   *
   * @param value new last name
   */
  public void setLastName(final String value) {
    this.lastName = value;
  }

  /**
   * Updates the username after profile save.
   *
   * @param value new username
   */
  public void setUsername(final String value) {
    this.username = value;
  }

  /**
   * Updates the email after profile save.
   *
   * @param value new email
   */
  public void setEmail(final String value) {
    this.email = value;
  }

  /**
   * Updates the role.
   *
   * @param value new role string
   */
  public void setRole(final String value) {
    this.role = value;
  }

  // ── State checks ──────────────────────────────────────────
  /**
   * Returns whether a user is currently logged in.
   *
   * @return true if the session holds a valid username
   */
  public boolean isLoggedIn() {
    return username != null && !username.isEmpty();
  }

  /**
   * Returns whether the current user has the ADMIN role.
   *
   * @return true if role equals ADMIN (case-insensitive)
   */
  public boolean isAdmin() {
    return "ADMIN".equalsIgnoreCase(role);
  }

  /**
   * Returns whether the current user has the SELLER role.
   *
   * @return true if role equals SELLER (case-insensitive)
   */
  public boolean isSeller() {
    return "SELLER".equalsIgnoreCase(role);
  }

  /**
   * Returns whether the current user has the BIDDER role.
   *
   * @return true if role equals BIDDER (case-insensitive)
   */
  public boolean isBidder() {
    return "BIDDER".equalsIgnoreCase(role);
  }

  // ── UI helpers ────────────────────────────────────────────
  /**
   * Returns 1-2 uppercase initials for the avatar pill in the top nav.
   *
   * <p>Examples: "John" + "Doe" returns "JD", "Nguyen" + "Anh" returns "NA",
   * "Alice" + "" returns "AL".
   *
   * @return initials string, or "?" if not logged in
   */
  public String getInitials() {
    if (!isLoggedIn()) {
      return "?";
    }
    final String first = firstName != null ? firstName.trim() : "";
    final String last = lastName != null ? lastName.trim() : "";

    if (first.isEmpty() && last.isEmpty()) {
      return username.substring(0, Math.min(2, username.length()))
          .toUpperCase();
    }
    if (last.isEmpty()) {
      return first.substring(0, Math.min(2, first.length())).toUpperCase();
    }
    return (String.valueOf(first.charAt(0))
        + String.valueOf(last.charAt(0))).toUpperCase();
  }

  /**
   * Returns the full display name ("First Last"), falling back to username.
   *
   * @return display name, or "Guest" if not logged in
   */
  public String getDisplayName() {
    if (!isLoggedIn()) {
      return "Guest";
    }
    final String first = firstName != null ? firstName.trim() : "";
    final String last = lastName != null ? lastName.trim() : "";
    if (first.isEmpty() && last.isEmpty()) {
      return username;
    }
    if (last.isEmpty()) {
      return first;
    }
    return first + " " + last;
  }

  /**
   * Returns the wallet balance.
   *
   * @return the wallet balance
   */
  public double getWalletBalance() {
    return walletBalance;
  }

  /**
   * Sets the wallet balance.
   *
   * @param balance the new wallet balance
   */
  public void setWalletBalance(final double balance) {
    this.walletBalance = balance;
  }
}
