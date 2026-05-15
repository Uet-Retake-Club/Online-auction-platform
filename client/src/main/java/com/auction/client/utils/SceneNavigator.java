package com.auction.client.utils;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * SceneNavigator is the central hub for screen navigation.
 *
 * <p>Key design decisions:
 * <ul>
 * <li>{@code sizeToScene()} is intentionally not called.</li>
 * <li>{@code centerOnScreen()} is intentionally not called.</li>
 * <li>Stage size is preserved while FXML roots fill the available window.
 * </ul>
 *
 * <p>Usage from any controller:
 * <pre>
 * SceneNavigator.navigateTo(SceneNavigator.View.HOME);
 * </pre>
 */
public final class SceneNavigator {

  private static final int FADE_OUT_MS = 120;
  private static final int FADE_IN_MS = 150;
  private static Stage stage;

  private SceneNavigator() { }

  /** All navigable screens in the application. */
  public enum View {
    /** Login screen. */
    LOGIN("/com/auction/client/views/LoginView.fxml"),
    /** Sign-up screen. */
    SIGNUP("/com/auction/client/views/SignUpView.fxml"),
    /** Home / browse screen. */
    HOME("/com/auction/client/views/HomeView.fxml"),
    /** Auction detail + bidding screen. */
    AUCTION_DETAIL("/com/auction/client/views/AuctionDetailView.fxml"),
    /** Create listing screen (Seller). */
    CREATE_LISTING("/com/auction/client/views/CreateListingView.fxml"),
    /** My bids history screen. */
    MY_BIDS("/com/auction/client/views/MyBidsView.fxml"),
    /** Admin dashboard. */
    ADMIN("/com/auction/client/views/AdminView.fxml"),
    /** Seller dashboard. */
    SELLER("/com/auction/client/views/SellerView.fxml"),
    /** User profile screen. */
    PROFILE("/com/auction/client/views/ProfileView.fxml");

    public final String path;

    View(final String fxmlPath) {
      this.path = fxmlPath;
    }
  }

  /**
   * Initialises the navigator with the application's primary Stage.
   * Must be called once in {@code ClientApplication.start()} before
   * any call to {@link #navigateTo(View)}.
   *
   * @param primaryStage the JavaFX primary stage
   */
  public static void init(final Stage primaryStage) {
    stage = primaryStage;
  }

  /**
   * Navigates to the given screen with a smooth fade transition.
   *
   * @param view the target screen to navigate to
   */
  public static void navigateTo(final View view) {
    try {
      final Parent root = FXMLLoader.load(
          SceneNavigator.class.getResource(view.path)
      );

      if (stage.getScene() == null) {
        final Scene scene = new Scene(root);
        scene.getStylesheets().add(
            SceneNavigator.class
                .getResource("/com/auction/client/styles/main.css")
                .toExternalForm()
        );
        stage.setScene(scene);
      } else {
        final Parent oldRoot = stage.getScene().getRoot();
        final FadeTransition fadeOut = 
            new FadeTransition(Duration.millis(FADE_OUT_MS), oldRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
          stage.getScene().setRoot(root);
          root.setOpacity(0);
          final FadeTransition fadeIn = 
              new FadeTransition(Duration.millis(FADE_IN_MS), root);
          fadeIn.setFromValue(0.0);
          fadeIn.setToValue(1.0);
          fadeIn.play();
        });
        fadeOut.play();
      }

    } catch (Exception ex) {
      System.err.println("Navigation error → " + view.path);
      ex.printStackTrace();
    }
  }

  /**
   * Navigates to the correct home screen based on the logged-in user's role.
   */
  public static void navigateAfterLogin() {
    if (UserSession.getInstance().isAdmin()) {
      navigateTo(View.ADMIN);
    } else if (UserSession.getInstance().isSeller()) {
      navigateTo(View.SELLER);
    } else {
      navigateTo(View.HOME);
    }
  }
}