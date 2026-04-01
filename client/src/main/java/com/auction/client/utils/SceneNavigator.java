package com.auction.client.utils;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * SceneNavigator.java
 * ─────────────────────────────────────────────
 * Central hub for ALL screen navigation.
 * Call SceneNavigator.navigateTo(View.HOME) from any controller.
 *
 * HOW TO USE:
 *   1. In ClientApplication.java  →  SceneNavigator.init(primaryStage);
 *   2. In any controller          →  SceneNavigator.navigateTo(View.HOME);
 */
public class SceneNavigator {

    // ── All screens in the app ───────────────────────────────
    public enum View {
        LOGIN   ("/com/auction/client/views/LoginView.fxml"),
        SIGNUP  ("/com/auction/client/views/SignUpView.fxml"),
        HOME    ("/com/auction/client/views/HomeView.fxml");

        public final String path;
        View(String path) { this.path = path; }
    }

    private static Stage stage;

    /** Call once in ClientApplication.start() */
    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    /**
     * Navigate to a screen with a smooth 150ms fade transition.
     * Safe to call from any controller — no boilerplate needed.
     */
    public static void navigateTo(View view) {
        try {
            Parent root = FXMLLoader.load(
                SceneNavigator.class.getResource(view.path)
            );

            // If scene doesn't exist yet, create it
            if (stage.getScene() == null) {
                Scene scene = new Scene(root);
                scene.getStylesheets().add(
                    SceneNavigator.class
                        .getResource("/com/auction/client/styles/main.css")
                        .toExternalForm()
                );
                stage.setScene(scene);
            } else {
                // Fade out → swap root → fade in
                Parent oldRoot = stage.getScene().getRoot();
                FadeTransition fadeOut = new FadeTransition(Duration.millis(120), oldRoot);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    stage.getScene().setRoot(root);
                    root.setOpacity(0);
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(150), root);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();
                });
                fadeOut.play();
            }

            // Resize window to match the new screen
            stage.sizeToScene();
            stage.centerOnScreen();

        } catch (Exception e) {
            System.err.println("Navigation error → " + view.path);
            e.printStackTrace();
        }
    }
}