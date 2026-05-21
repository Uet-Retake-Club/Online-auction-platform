package com.auction.client.utils;

import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Utility class for displaying inline notifications within a JavaFX scene.
 */
public final class InlineNotification {

  private InlineNotification() {}

  /**
   * Displays an inline notification inside a VBox.
   *
   * @param box       The VBox container for the notification.
   * @param label     The Label inside the VBox to show the text.
   * @param msg       The message to display.
   * @param isSuccess True for success style, false for error/warning style.
   */
  public static void show(final VBox box, final Label label, final String msg,
      final boolean isSuccess) {
    label.setText(msg);
    if (isSuccess) {
      box.setStyle("-fx-background-color:#EAF5EA;-fx-padding:10px;"
          + "-fx-border-radius:6px;-fx-background-radius:6px;-fx-margin:0 0 10px 0;");
      label.setStyle("-fx-font-size:13px;-fx-font-weight:bold;"
          + "-fx-text-fill:#5BA55B;-fx-wrap-text:true;");
    } else {
      box.setStyle("-fx-background-color:#FDECEA;-fx-padding:10px;"
          + "-fx-border-radius:6px;-fx-background-radius:6px;-fx-margin:0 0 10px 0;");
      label.setStyle("-fx-font-size:13px;-fx-font-weight:bold;"
          + "-fx-text-fill:#E53238;-fx-wrap-text:true;");
    }
    box.setVisible(true);
    box.setManaged(true);

    final PauseTransition pause = new PauseTransition(Duration.seconds(4));
    pause.setOnFinished(e -> {
      box.setVisible(false);
      box.setManaged(false);
    });
    pause.play();
  }
}
