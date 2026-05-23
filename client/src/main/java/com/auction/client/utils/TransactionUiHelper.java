package com.auction.client.utils;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Helper to build transaction UI rows for the Wallet screen.
 */
public final class TransactionUiHelper {

  /** Utility class — do not instantiate. */
  private TransactionUiHelper() {
    // no-op
  }

  /**
   * Builds an HBox representing a single transaction row.
   *
   * @param desc  transaction description
   * @param amt   formatted amount string (e.g. "+$500.00")
   * @param date  date string
   * @param type  transaction type constant
   * @param last  whether this is the last row (suppresses bottom border)
   * @return the assembled HBox node
   */
  public static HBox buildTxRow(
      final String desc,
      final String amt,
      final String date,
      final String type,
      final boolean last) {

    final HBox row = new HBox(12);
    row.setAlignment(Pos.CENTER_LEFT);
    row.setStyle("-fx-padding:12px 14px;"
        + (last ? "" : "-fx-border-color:transparent transparent -border-color transparent;"
            + "-fx-border-width:0 0 1px 0;"));

    final Label icon = new Label(getIcon(type));
    icon.setStyle("-fx-min-width:36px;-fx-min-height:36px;-fx-background-radius:18px;"
        + "-fx-alignment:center;-fx-font-weight:bold;");
    icon.getStyleClass().add(getIconStyleClass(type));

    final Label descLabel = new Label(desc);
    descLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;");

    final Label dateLabel = new Label(date);
    dateLabel.setStyle("-fx-font-size:11px;-fx-text-fill:-text-secondary;");

    final VBox body = new VBox(2, descLabel, dateLabel);
    HBox.setHgrow(body, Priority.ALWAYS);

    final Label amtLabel = new Label(amt);
    amtLabel.setStyle("-fx-font-weight:bold;"
        + (amt.startsWith("+") ? "-fx-text-fill:-success;" : "-fx-text-fill:-primary;"));

    row.getChildren().addAll(icon, body, amtLabel);
    return row;
  }

  /**
   * Returns the icon character for a transaction type.
   *
   * @param type transaction type
   * @return single-character icon string
   */
  private static String getIcon(final String type) {
    return switch (type) {
      case "deposit" -> "+";
      case "hold"    -> "🔒";
      case "refund"  -> "↩";
      default        -> "↑";
    };
  }

  /**
   * Returns the style class for the icon badge based on transaction type.
   *
   * @param type transaction type
   * @return style class string
   */
  private static String getIconStyleClass(final String type) {
    return switch (type) {
      case "deposit", "refund" -> "badge-success";
      case "hold"              -> "badge-warning";
      default                  -> "badge-danger";
    };
  }
}