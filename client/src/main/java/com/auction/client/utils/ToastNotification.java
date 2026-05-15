package com.auction.client.utils;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * ToastNotification displays short transient messages anchored to the
 * bottom-right corner of the current window. Text wraps automatically and
 * the popup is clamped so it never overflows the screen.
 */
public final class ToastNotification {

  /** Toast message types. */
  public enum Type {
    SUCCESS, WARNING, DANGER, INFO
  }

  private static final double TOAST_WIDTH   = 340;
  private static final double MARGIN        = 20;
  private static final double DISPLAY_SECS  = 3.0;
  private static final double FADE_MILLIS   = 200;
  private static final double SLIDE_MILLIS  = 180;

  private ToastNotification() { }

  /**
   * Shows a toast message anchored to the provided node's window,
   * positioned at the bottom-right and clamped within screen bounds.
   *
   * @param anchor  view node used to resolve the window
   * @param message text to display
   * @param type    visual style of the toast
   */
  public static void show(final Node anchor, final String message, final Type type) {
    if (anchor == null || anchor.getScene() == null) {
      return;
    }
    Platform.runLater(() -> {
      final Window window = anchor.getScene().getWindow();
      if (window == null) return;

      // ── Build label ───────────────────────────────────────────
      final Label icon = new Label(iconFor(type));
      icon.setStyle("-fx-font-size: 15px; -fx-padding: 0 6 0 0;");

      final Label text = new Label(message);
      text.setWrapText(true);
      text.setMaxWidth(TOAST_WIDTH - 70);
      text.setStyle("-fx-font-size: 13px; -fx-text-fill: white; -fx-font-family: 'Segoe UI';");

      final HBox row = new HBox(icon, text);
      row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
      row.setPadding(new Insets(12, 18, 12, 14));
      row.setMaxWidth(TOAST_WIDTH);
      row.setMinWidth(220);
      row.setStyle(containerStyle(type));
      row.setOpacity(0);

      final VBox container = new VBox(row);

      final Popup popup = new Popup();
      popup.setAutoFix(false);   // we position manually
      popup.setAutoHide(true);
      popup.setHideOnEscape(true);
      popup.getContent().add(container);

      // ── Position: bottom-right of the window ──────────────────
      final double wx = window.getX();
      final double wy = window.getY();
      final double ww = window.getWidth();
      final double wh = window.getHeight();

      final double x = wx + ww - TOAST_WIDTH - MARGIN;
      // Show above the taskbar area, 80 px from bottom
      final double y = wy + wh - 100;

      popup.show(window, x, y);

      // ── Slide-up + fade in ─────────────────────────────────────
      row.setTranslateY(20);
      final TranslateTransition slideIn =
          new TranslateTransition(Duration.millis(SLIDE_MILLIS), row);
      slideIn.setFromY(20);
      slideIn.setToY(0);

      final FadeTransition fadeIn =
          new FadeTransition(Duration.millis(FADE_MILLIS), row);
      fadeIn.setFromValue(0);
      fadeIn.setToValue(1);

      new ParallelTransition(slideIn, fadeIn).play();

      // ── Auto-dismiss ───────────────────────────────────────────
      final PauseTransition pause = new PauseTransition(Duration.seconds(DISPLAY_SECS));
      pause.setOnFinished(event -> {
        final FadeTransition fadeOut =
            new FadeTransition(Duration.millis(FADE_MILLIS), row);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> popup.hide());
        fadeOut.play();
      });
      pause.play();
    });
  }

  private static String iconFor(final Type type) {
    return switch (type) {
      case SUCCESS -> "[SUCCESS]";
      case WARNING -> "[WARNING]";
      case DANGER  -> "[ERROR]";
      default      -> "[INFO]";
    };
  }

  private static String containerStyle(final Type type) {
    final String bg = switch (type) {
      case SUCCESS -> "#2E7D32";
      case WARNING -> "#E65100";
      case DANGER  -> "#C62828";
      default      -> "#1A237E";
    };
    return "-fx-background-color: " + bg + ";"
        + "-fx-background-radius: 10px;"
        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 14, 0, 0, 4);";
  }
}
