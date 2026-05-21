package com.auction.client.utils;

import com.auction.client.services.BidService;
import com.auction.client.services.NetworkClientService;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import java.util.function.Consumer;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

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

  /**
   * Binds a navigation wallet balance label to receive updates from BidService.
   * Automatically cleans up the listener when the label is removed from the scene.
   *
   * @param walletLabel the label showing the wallet balance
   */
  public static void bindWalletBalance(final Label walletLabel) {
    if (walletLabel == null) {
      return;
    }
    // Set initial balance
    walletLabel.setText(formatMoney(UserSession.getInstance().getWalletBalance()));

    // Create and register the listener
    final Consumer<Double> listener = (Double balance) -> {
      walletLabel.setText(formatMoney(balance));
    };
    BidService.getInstance().addWalletBalanceListener(listener);

    // Auto-remove listener on node removal to avoid memory leaks
    walletLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
      if (newScene == null) {
        BidService.getInstance().removeWalletBalanceListener(listener);
      }
    });

    // Request the latest wallet balance from the server to ensure UI is fresh
    NetworkClientService.getInstance().sendRequest(
        new Request(MessageType.GET_WALLET_BALANCE,
            UserSession.getInstance().getUserId(), ""));
  }
}
