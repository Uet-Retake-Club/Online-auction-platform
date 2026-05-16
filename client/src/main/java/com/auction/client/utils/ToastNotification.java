package com.auction.client.utils;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * ToastNotification displays short transient messages anchored to the
 * bottom-right corner of the current window. Supports SUCCESS, WARNING,
 * DANGER, and INFO types with coloured pill styling and slide-up animation.
 */
public final class ToastNotification {

  /** Toast message types. */
  public enum Type {
    SUCCESS, WARNING, DANGER, INFO
  }

  private static final double TOAST_WIDTH  = 280;
  private static final double MARGIN       = 20;
  private static final double DISPLAY_SECS = 2.8;
  private static final double FADE_MILLIS  = 180;
  private static final double SLIDE_MILLIS = 180;

  private ToastNotification() { }

  /**
   * Shows a toast anchored to the provided node's window,
   * positioned at the bottom-right, clamped within screen bounds.
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

      // ── Icon circle ────────────────────────────────────────────
      final Label icon = new Label(iconFor(type));
      icon.setStyle(
          "-fx-font-size: 12px;"
          + "-fx-text-fill: white;"
          + "-fx-font-family: 'Segoe UI';"
          + "-fx-padding: 0;"
          + "-fx-min-width: 24px;"
          + "-fx-pref-width: 24px;"
          + "-fx-alignment: center;"
      );

      // ── Message text ───────────────────────────────────────────
      final Label text = new Label(message);
      text.setWrapText(true);
      text.setMaxWidth(TOAST_WIDTH - 64);
      text.setStyle(
          "-fx-font-size: 12px;"
          + "-fx-text-fill: white;"
          + "-fx-font-family: 'Segoe UI';"
      );
      HBox.setHgrow(text, Priority.ALWAYS);

      // ── Separator accent line ───────────────────────────────────
      final Region accent = new Region();
      accent.setStyle("-fx-background-color: " + accentColor(type) + "; -fx-min-width: 3px; -fx-pref-width: 3px; -fx-background-radius: 3px 0 0 3px;");

      // ── Row layout ─────────────────────────────────────────────
      final HBox row = new HBox(10);
      row.setAlignment(Pos.CENTER_LEFT);
      row.getChildren().addAll(icon, text);
      row.setPadding(new Insets(10, 14, 10, 10));

      // ── Outer container: accent + content ──────────────────────
      final HBox toast = new HBox(0);
      toast.getChildren().addAll(accent, row);
      HBox.setHgrow(row, Priority.ALWAYS);
      toast.setMaxWidth(TOAST_WIDTH);
      toast.setMinWidth(240);
      toast.setStyle(containerStyle(type));
      toast.setOpacity(0);

      // ── Wrap in a StackPane so Popup has no white background ───
      // The StackPane must be transparent; the colour lives on `toast` only.
      final StackPane wrapper = new StackPane(toast);
      wrapper.setStyle("-fx-background-color: transparent;");
      wrapper.setPadding(new Insets(0));

      final Popup popup = new Popup();
      popup.setAutoFix(false);
      popup.setAutoHide(true);
      popup.setHideOnEscape(true);
      popup.getContent().add(wrapper);

      // ── Position: bottom-right of the window ─────────────────
      final double wx = window.getX();
      final double wy = window.getY();
      final double ww = window.getWidth();
      final double wh = window.getHeight();

      // Estimate toast height before it is rendered
      final double estimatedHeight = 68;
      final double x = wx + ww - TOAST_WIDTH - MARGIN;
      final double y = wy + wh - estimatedHeight - MARGIN - 32; // above taskbar area

      popup.show(window, x, y);

      // ── Slide-up + fade in ─────────────────────────────────────
      toast.setTranslateY(16);
      final TranslateTransition slideIn =
          new TranslateTransition(Duration.millis(SLIDE_MILLIS), toast);
      slideIn.setFromY(16);
      slideIn.setToY(0);

      final FadeTransition fadeIn =
          new FadeTransition(Duration.millis(FADE_MILLIS), toast);
      fadeIn.setFromValue(0);
      fadeIn.setToValue(1);

      new ParallelTransition(slideIn, fadeIn).play();

      // ── Auto-dismiss after DISPLAY_SECS ───────────────────────
      final PauseTransition pause =
          new PauseTransition(Duration.seconds(DISPLAY_SECS));
      pause.setOnFinished(event -> {
        final FadeTransition fadeOut =
            new FadeTransition(Duration.millis(FADE_MILLIS), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> popup.hide());
        fadeOut.play();
      });
      pause.play();
    });
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private static String iconFor(final Type type) {
    return switch (type) {
      case SUCCESS -> "✓";
      case WARNING -> "⚠";
      case DANGER  -> "✕";
      default      -> "ℹ";
    };
  }

  /** Returns the darker left-accent colour per type. */
  private static String accentColor(final Type type) {
    return switch (type) {
      case SUCCESS -> "#1B5E20";
      case WARNING -> "#BF360C";
      case DANGER  -> "#7F0000";
      default      -> "#0D47A1";
    };
  }

  private static String containerStyle(final Type type) {
    final String bg = switch (type) {
      case SUCCESS -> "#2E7D32";
      case WARNING -> "#E65100";
      case DANGER  -> "#C62828";
      default      -> "#1565C0";
    };
    return "-fx-background-color: " + bg + ";"
        + "-fx-background-radius: 10px;"
        + "-fx-border-radius: 10px;"
        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.40), 18, 0, 0, 6);";
  }
}
