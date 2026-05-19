package com.auction.client.utils;

import javafx.scene.control.Button;

/**
 * Utility helpers for top navigation controls such as wallet balance display.
 */
public final class TopNavUtils {

  private TopNavUtils() { }

  /**
   * Formats a numeric amount as US currency with grouping and two decimals.
   *
   * @param amount the amount to format
   * @return a formatted currency string like {@code $1,234.56}
   */
  public static String formatMoney(final double amount) {
    return String.format("$%,.2f", amount);
  }

  /**
   * Updates the text of a top navigation wallet button using the current session balance.
   *
   * <p>If the provided button is {@code null}, this method returns without modifying state.</p>
   *
   * @param walletButton the button that should show the wallet balance
   */
  public static void updateWalletBalance(final Button walletButton) {
    if (walletButton == null) {
      return;
    }
    walletButton.setText("Wallet: "
        + formatMoney(UserSession.getInstance().getWalletBalance()));
  }
}
