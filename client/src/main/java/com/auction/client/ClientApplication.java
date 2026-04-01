package com.auction.client;

import com.auction.client.utils.SceneNavigator;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * ClientApplication.java
 * ─────────────────────────────────────────────
 * JavaFX entry point.
 * Initialises SceneNavigator then opens the Login screen.
 */
public class ClientApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. Hand the stage to SceneNavigator — do this FIRST
        SceneNavigator.init(primaryStage);

        // 2. Window settings
        primaryStage.setTitle("AuctionHub");
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(500);

        // 3. Open the login screen — CSS is loaded inside SceneNavigator
        SceneNavigator.navigateTo(SceneNavigator.View.LOGIN);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}