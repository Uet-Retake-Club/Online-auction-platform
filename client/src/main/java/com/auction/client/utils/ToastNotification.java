package com.auction.client.utils;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * ToastNotification displays short transient messages near the current window.
 */
public final class ToastNotification {

  /** Toast message types. */
  public enum Type {
    SUCCESS, WARNING, DANGER, INFO
  }

  private ToastNotification() { }

  /**
   * Shows a toast message anchored to the provided node.
   *
   * @param anchor view node used to locate the toast window
   * @param message text to display
   * @param type visual style of the toast
   */
  public static void show(final Node anchor, final String message, final Type type) {
    if (anchor == null || anchor.getScene() == null) {
      return;
    }
    Platform.runLater(() -> {
      Window window = anchor.getScene().getWindow();
      if (window == null) {
        return;
      }

      Label toast = new Label(message);
      toast.setWrapText(true);
      toast.setMaxWidth(320);
      toast.setPadding(new Insets(12, 18, 12, 18));
      toast.setStyle(getToastStyle(type));
      toast.setOpacity(0);

      Popup popup = new Popup();
      popup.setAutoFix(true);
      popup.setAutoHide(true);
      popup.setHideOnEscape(true);
      popup.getContent().add(toast);

      double x = window.getX() + window.getWidth() - 360;
      double y = window.getY() + 24;
      popup.show(window, x, y);

      FadeTransition fadeIn = new FadeTransition(Duration.millis(180), toast);
      fadeIn.setFromValue(0);
      fadeIn.setToValue(1);
      fadeIn.play();

      PauseTransition pause = new PauseTransition(Duration.seconds(2.5));
      pause.setOnFinished(event -> {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(180), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> popup.hide());
        fadeOut.play();
      });
      pause.play();
    });
  }

  /**
   * Returns the style string for the toast message.
   *
   * @param type toast type
   * @return inline CSS style
   */
  private static String getToastStyle(final Type type) {
    String background;
    switch (type) {
      case SUCCESS -> background = "#5BA55B";
      case WARNING -> background = "#F5A623";
      case DANGER  -> background = "#E53238";
      default      -> background = "#323232";
    }
    return "-fx-background-color: " + background + ";"
       + "-fx-text-fill: white;"
       + "-fx-font-size: 13px;"
       + "-fx-background-radius: 10px;"
       + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.22), 8, 0, 0, 2);";
  }
}
