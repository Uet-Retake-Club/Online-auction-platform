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
 * Trung tâm điều hướng toàn bộ ứng dụng.
 * Gọi SceneNavigator.navigateTo(View.HOME) từ bất kỳ controller nào.
 *
 * Cách dùng:
 *   1. Trong ClientApplication.java  →  SceneNavigator.init(primaryStage);
 *   2. Trong bất kỳ controller nào   →  SceneNavigator.navigateTo(View.HOME);
 */
public class SceneNavigator {

    /** Danh sách toàn bộ màn hình của ứng dụng */
    public enum View {
        LOGIN          ("/com/auction/client/views/LoginView.fxml"),
        SIGNUP         ("/com/auction/client/views/SignUpView.fxml"),
        HOME           ("/com/auction/client/views/HomeView.fxml"),
        AUCTION_DETAIL ("/com/auction/client/views/AuctionDetailView.fxml"),
        CREATE_LISTING ("/com/auction/client/views/CreateListingView.fxml"),
        MY_BIDS        ("/com/auction/client/views/MyBidsView.fxml"),
        ADMIN          ("/com/auction/client/views/AdminView.fxml");

        public final String path;
        View(String path) { this.path = path; }
    }

    private static Stage stage;

    /** Gọi một lần trong ClientApplication.start() */
    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    /**
     * Chuyển màn hình với hiệu ứng fade mượt 120ms → 150ms.
     * An toàn khi gọi từ bất kỳ controller nào.
     */
    public static void navigateTo(View view) {
        try {
            Parent root = FXMLLoader.load(
                SceneNavigator.class.getResource(view.path)
            );

            if (stage.getScene() == null) {
                // Tạo Scene lần đầu — gắn main.css toàn cục
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

            stage.sizeToScene();
            stage.centerOnScreen();

        } catch (Exception e) {
            System.err.println("Lỗi điều hướng → " + view.path);
            e.printStackTrace();
        }
    }
}