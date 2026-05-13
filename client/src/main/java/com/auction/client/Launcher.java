package com.auction.client;

/**
 * Launcher.java
 * Separate launcher class required by JavaFX + Gradle.
 * The main class must NOT extend Application directly
 * when using the Gradle JavaFX plugin — this wrapper fixes that.
 */
public class Launcher {

  public static void main(String[] args) {
    // Delegate to the real JavaFX Application class
    ClientApplication.main(args);
  }
}