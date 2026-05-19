package com.auction.client.utils;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Utility for showing transient toast-style notifications inside the active JavaFX scene.
 * <p>
 * The notification is rendered as a temporary overlay inside the nearest {@link StackPane}
 * ancestor of the given {@code anchor} node. This keeps the message within the current window
 * and avoids creating separate pop-up windows.
 * </p>
 */
public final class ToastNotification {

  /**
   * Notification appearance variants.
   */
  public enum Type {
    SUCCESS, WARNING, DANGER, INFO
  }

  private ToastNotification() { }

  /**
   * Shows a brief toast message anchored to the current scene.
   *
   * @param anchor the node from which the scene is resolved; used to locate the root overlay pane
   * @param message the message text to display inside the toast
   * @param type the visual style variant for the toast notification
   */
  public static void show(final Node anchor, final String message, final Type type) {
    if (anchor == null || anchor.getScene() == null) {
      return;
    }
    Platform.runLater(() -> {
      final StackPane root = findStackPane(anchor.getScene().getRoot());
      if (root == null) {
        return;
      }

      final Label toast = new Label(message);
      toast.setWrapText(true);
      toast.setMaxWidth(320);
      toast.setPadding(new Insets(12, 18, 12, 18));
      toast.setStyle(getToastStyle(type));
      toast.setOpacity(0);

      final VBox container = new VBox(toast);
      container.setAlignment(Pos.TOP_RIGHT);
      container.setMouseTransparent(true);
      container.setPadding(new Insets(18, 20, 0, 0));
      container.setOpacity(0);

      root.getChildren().add(container);

      final FadeTransition fadeIn = new FadeTransition(Duration.millis(180), container);
      fadeIn.setFromValue(0);
      fadeIn.setToValue(1);
      fadeIn.play();

      final PauseTransition pause = new PauseTransition(Duration.seconds(2.5));
      pause.setOnFinished(event -> {
        final FadeTransition fadeOut = new FadeTransition(Duration.millis(180), container);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> root.getChildren().remove(container));
        fadeOut.play();
      });
      pause.play();
    });
  }

  /**
   * Walks the scene graph to find the first {@link StackPane} ancestor.
   *
   * @param parent the parent node to search
   * @return the first {@link StackPane} found, or {@code null} when none exists
   */
  private static StackPane findStackPane(final Parent parent) {
    if (parent instanceof StackPane stack) {
      return stack;
    }
    for (final javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
      if (child instanceof Parent nested) {
        final StackPane found = findStackPane(nested);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  /**
   * Returns the CSS style string for the toast container based on type.
   *
   * @param type the toast notification style variant
   * @return CSS style data used to render the toast background and text
   */
  private static String getToastStyle(final Type type) {
    final String background;
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
