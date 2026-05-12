package com.auction.client.utils;

import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

/**
 * ConfirmBidDialog displays a confirmation alert for bid actions.
 */
public final class ConfirmBidDialog {

  private ConfirmBidDialog() { }

  /**
   * Shows a confirmation dialog.
   *
   * @param title dialog title
   * @param message dialog message
   * @return true if the user confirms the action
   */
  public static boolean show(final String title, final String message) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);

    ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
    ButtonType confirm = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
    alert.getButtonTypes().setAll(cancel, confirm);

    Optional<ButtonType> result = alert.showAndWait();
    return result.isPresent() && result.get() == confirm;
  }
}
