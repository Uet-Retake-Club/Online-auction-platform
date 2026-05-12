package com.auction.client;

import com.auction.client.utils.SceneNavigator;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * ClientApplication is the JavaFX entry point for the client UI.
 *
 * <p>Stage owns the window size. FXML files use maxWidth/maxHeight="Infinity"
 * to fill whatever size the Stage currently has. This prevents the window from
 * resizing when navigating between screens.
 */
public class ClientApplication extends Application {

  /**
   * Default window width on startup.
   */
  private static final double DEFAULT_WIDTH = 1100;

  /**
   * Default window height on startup.
   */
  private static final double DEFAULT_HEIGHT = 700;

  /**
   * Minimum allowed window width.
   */
  private static final double MIN_WIDTH = 800;

  /**
   * Minimum allowed window height.
   */
  private static final double MIN_HEIGHT = 560;

  @Override
  public void start(final Stage primaryStage) {
    // 1. Register Stage with SceneNavigator first — everything depends on this
    SceneNavigator.init(primaryStage);

    // 2. Stage owns the window dimensions — set once, never changed on navigate
    primaryStage.setTitle("AuctionHub");
    primaryStage.setWidth(DEFAULT_WIDTH);
    primaryStage.setHeight(DEFAULT_HEIGHT);
    primaryStage.setMinWidth(MIN_WIDTH);
    primaryStage.setMinHeight(MIN_HEIGHT);

    // 3. Load first screen — CSS is attached inside SceneNavigator.navigateTo()
    SceneNavigator.navigateTo(SceneNavigator.View.LOGIN);

    // 4. Centre on screen after size is set, then show
    primaryStage.centerOnScreen();
    primaryStage.show();
  }

  /**
   * Application entry point.
   *
   * @param args command-line arguments
   */
  public static void main(final String[] args) {
    launch(args);
  }
}