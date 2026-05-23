package com.auction.client.utils;

import java.net.URL;
import java.util.logging.Logger;
import java.util.logging.Level;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import javafx.scene.layout.StackPane;
import com.auction.client.controllers.MainLayoutController;

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

  private static final Logger LOGGER = Logger.getLogger(SceneNavigator.class.getName());
  private static final int FADE_OUT_MS = 120;
  private static final int FADE_IN_MS = 150;
  private static Stage stage;
  private static Parent mainLayoutRoot;
  private static MainLayoutController mainLayoutController;

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
    /** Wallet screen. */
    WALLET("/com/auction/client/views/WalletView.fxml"),
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

  private static boolean isDarkMode = false;

  /**
   * Toggles the global theme between light and dark mode.
   */
  public static void toggleTheme() {
    isDarkMode = !isDarkMode;
    if (stage.getScene() != null && stage.getScene().getRoot() != null) {
      applyTheme(stage.getScene().getRoot());
    }
  }

  /**
   * Applies the current theme to the given root node.
   *
   * @param root the node to apply the theme to
   */
  private static void applyTheme(final Parent root) {
    if (isDarkMode) {
      if (!root.getStyleClass().contains("dark-theme")) {
        root.getStyleClass().add("dark-theme");
      }
    } else {
      root.getStyleClass().remove("dark-theme");
    }
  }

  /**
   * Navigates to the given screen with a smooth fade transition.
   *
   * @param view the target screen to navigate to
   */
  public static void navigateTo(final View view) {
    System.out.println("NAVIGATING TO: " + view.name() + " (" + view.path + ")");
    try {
      final URL fxmlUrl = SceneNavigator.class.getResource(view.path);
      if (fxmlUrl == null) {
        System.err.println("FATAL: FXML file not found at path: " + view.path);
        return;
      }
      
      final FXMLLoader loader = new FXMLLoader(fxmlUrl);
      final Parent root = loader.load();
      
      final String css = SceneNavigator.class.getResource("/com/auction/client/styles/main.css").toExternalForm();
      root.getStylesheets().add(css);
      applyTheme(root);

      boolean isAuthScreen = view == View.LOGIN || view == View.SIGNUP;

      if (isAuthScreen) {
        if (stage.getScene() == null) {
          final Scene scene = new Scene(root);
          stage.setScene(scene);
        } else {
          switchRootWithFade(stage.getScene().getRoot(), root, () -> stage.getScene().setRoot(root));
        }
      } else {
        if (mainLayoutRoot == null) {
          FXMLLoader shellLoader = new FXMLLoader(SceneNavigator.class.getResource("/com/auction/client/views/MainLayout.fxml"));
          mainLayoutRoot = shellLoader.load();
          mainLayoutRoot.getStylesheets().add(css);
          mainLayoutController = shellLoader.getController();
          applyTheme(mainLayoutRoot);
        }

        if (stage.getScene() == null) {
          final Scene scene = new Scene(mainLayoutRoot);
          stage.setScene(scene);
        } else if (stage.getScene().getRoot() != mainLayoutRoot) {
          switchRootWithFade(stage.getScene().getRoot(), mainLayoutRoot, () -> stage.getScene().setRoot(mainLayoutRoot));
        }

        StackPane contentArea = mainLayoutController.getContentArea();
        mainLayoutController.updateNavigation();

        if (contentArea.getChildren().isEmpty()) {
            contentArea.getChildren().add(root);
        } else {
            Parent oldContent = (Parent) contentArea.getChildren().get(0);
            switchRootWithFade(oldContent, root, () -> {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(root);
            });
        }
      }

    } catch (Exception ex) {
      System.err.println("Navigation error -> " + view.path);
      ex.printStackTrace();
      if (ex.getCause() != null) {
        System.err.println("Caused by:");
        ex.getCause().printStackTrace();
      }
    }
  }

  private static void switchRootWithFade(Parent oldNode, Parent newNode, Runnable swapAction) {
    final FadeTransition fadeOut = new FadeTransition(Duration.millis(FADE_OUT_MS), oldNode);
    fadeOut.setFromValue(1.0);
    fadeOut.setToValue(0.0);
    fadeOut.setOnFinished(e -> {
      swapAction.run();
      newNode.setOpacity(0);
      final FadeTransition fadeIn = new FadeTransition(Duration.millis(FADE_IN_MS), newNode);
      fadeIn.setFromValue(0.0);
      fadeIn.setToValue(1.0);
      fadeIn.play();
    });
    fadeOut.play();
  }

  /**
   * Navigates to the correct home screen based on the logged-in user's role.
   */
  public static void navigateAfterLogin() {
    // Also reset mainLayout so it redraws with new user
    mainLayoutRoot = null;
    mainLayoutController = null;
    
    if (UserSession.getInstance().isAdmin()) {
      navigateTo(View.ADMIN);
    } else if (UserSession.getInstance().isSeller()) {
      navigateTo(View.SELLER);
    } else {
      navigateTo(View.HOME);
    }
  }
}