package com.auction.client.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import com.auction.client.utils.SceneNavigator;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * HomeController.java
 * ─────────────────────────────────────────────
 * Handles HomeView.fxml.
 * Fully navigable UI with dummy auction cards.
 * No backend needed — swap the dummy data for real data later.
 */
public class HomeController implements Initializable {

    // ── Top nav ──────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private Label     userLabel;

    // ── Sidebar ──────────────────────────────────────────────
    @FXML private Button allCategoriesBtn;
    @FXML private Button electronicsBtn;
    @FXML private Button fashionBtn;
    @FXML private Button homeGardenBtn;
    @FXML private Button sportsBtn;
    @FXML private Button collectiblesBtn;
    @FXML private Button vehiclesBtn;
    @FXML private Button otherBtn;

    // ── Content grids ────────────────────────────────────────
    @FXML private FlowPane endingSoonGrid;
    @FXML private FlowPane recentGrid;

    // ── States ───────────────────────────────────────────────
    @FXML private VBox emptyState;
    @FXML private VBox loadingState;

    // ── See-all links ────────────────────────────────────────
    @FXML private Label seeAllEndingSoon;
    @FXML private Label seeAllRecent;

    private Button activeCategory;

    // ── Dummy data records ───────────────────────────────────
    // Format: { title, price, bids, timeLeft, badgeType }
    // badgeType: "warning" = ending soon   "success" = winning
    private static final String[][] ENDING_SOON = {
        { "Vintage Rolex Watch",   "$1,240.00", "14 bids", "2h left",  "warning" },
        { "iPhone 15 Pro Max",     "$780.00",   "31 bids", "45m left", "warning" },
        { "Nike Air Jordan 1",     "$210.00",   "8 bids",  "WINNING",  "success" },
        { "Sony WH-1000XM5",       "$190.00",   "5 bids",  "1h left",  "warning" },
    };
    private static final String[][] RECENTLY_LISTED = {
        { "Gaming Chair RGB",      "$95.00",    "2 bids",  "3d left",  "warning" },
        { "MacBook Air M2",        "$850.00",   "0 bids",  "5d left",  "warning" },
        { "Lego Star Wars Set",    "$45.00",    "1 bid",   "4d left",  "warning" },
        { "Canon EOS R50",         "$620.00",   "3 bids",  "2d left",  "warning" },
    };

    // ── Lifecycle ────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        activeCategory = allCategoriesBtn;

        // Show user initials (replace "JD" with UserSession data later)
        userLabel.setText("JD");

        // Populate both grids with dummy cards
        populateGrid(endingSoonGrid, ENDING_SOON);
        populateGrid(recentGrid,     RECENTLY_LISTED);
    }

    // ── Search ───────────────────────────────────────────────

    @FXML
    private void onSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            showEmpty(false);
            populateGrid(endingSoonGrid, ENDING_SOON);
            populateGrid(recentGrid,     RECENTLY_LISTED);
        } else {
            // TODO: replace with real AuctionService.search(query)
            // For now: simulate "no results" if query is not empty
            endingSoonGrid.getChildren().clear();
            recentGrid.getChildren().clear();
            showEmpty(true);
        }
    }

    @FXML
    private void onClearSearch() {
        searchField.clear();
        showEmpty(false);
        populateGrid(endingSoonGrid, ENDING_SOON);
        populateGrid(recentGrid,     RECENTLY_LISTED);
    }

    // ── Category sidebar ─────────────────────────────────────

    @FXML private void onCategoryAll()          { switchCategory(allCategoriesBtn);  reloadAll(); }
    @FXML private void onCategoryElectronics()  { switchCategory(electronicsBtn);    reloadAll(); }
    @FXML private void onCategoryFashion()      { switchCategory(fashionBtn);        reloadAll(); }
    @FXML private void onCategoryHome()         { switchCategory(homeGardenBtn);     reloadAll(); }
    @FXML private void onCategorySports()       { switchCategory(sportsBtn);         reloadAll(); }
    @FXML private void onCategoryCollectibles() { switchCategory(collectiblesBtn);   reloadAll(); }
    @FXML private void onCategoryVehicles()     { switchCategory(vehiclesBtn);       reloadAll(); }
    @FXML private void onCategoryOther()        { switchCategory(otherBtn);          reloadAll(); }

    private void switchCategory(Button btn) {
        if (activeCategory != null) {
            activeCategory.getStyleClass().remove("nav-item-active");
            activeCategory.getStyleClass().add("nav-item");
        }
        btn.getStyleClass().remove("nav-item");
        btn.getStyleClass().add("nav-item-active");
        activeCategory = btn;
    }

    private void reloadAll() {
        // TODO: filter by selected category via AuctionService
        showEmpty(false);
        populateGrid(endingSoonGrid, ENDING_SOON);
        populateGrid(recentGrid,     RECENTLY_LISTED);
    }

    // ── Top-nav button handlers ──────────────────────────────

    @FXML private void onSell()      { System.out.println("TODO: navigate to CreateListingView"); }
    @FXML private void onWatchlist() { System.out.println("TODO: navigate to WatchlistView");    }
    @FXML private void onMyBids()    { System.out.println("TODO: navigate to MyBidsView");       }
    @FXML private void onProfile()   { System.out.println("TODO: navigate to ProfileView");      }

    @FXML
    private void onLogout() {
        // TODO: UserSession.getInstance().clear();
        SceneNavigator.navigateTo(SceneNavigator.View.LOGIN);
    }

    @FXML private void onSeeAllEndingSoon() { System.out.println("See all ending soon"); }
    @FXML private void onSeeAllRecent()     { System.out.println("See all recent");      }

    // ── Card builder ─────────────────────────────────────────

    /**
     * Builds a VBox auction card for each row of dummy data
     * and adds it into the given FlowPane grid.
     *
     * When your backend is ready, replace String[][] data with
     * List<Auction> and read real fields from each Auction object.
     */
    private void populateGrid(FlowPane grid, String[][] data) {
        grid.getChildren().clear();
        for (String[] item : data) {
            grid.getChildren().add(buildCard(item[0], item[1], item[2], item[3], item[4]));
        }
    }

    private VBox buildCard(String title, String price,
                           String bids,  String timeLeft, String badgeType) {

        // ── Image placeholder ────────────────────────────────
        StackPane imgBox = new StackPane();
        imgBox.setPrefHeight(110);
        imgBox.setStyle("-fx-background-color: #F4F4F4;"
                      + "-fx-background-radius: 6px;");

        Label noImg = new Label("No image");
        noImg.setStyle("-fx-text-fill: #CCCCCC; -fx-font-size: 11px;"
                     + "-fx-font-family: 'Segoe UI';");
        imgBox.getChildren().add(noImg);

        // ── Title ────────────────────────────────────────────
        Label titleLabel = new Label(title);
        titleLabel.setMaxWidth(155);
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;"
                          + "-fx-text-fill: #111111; -fx-font-family: 'Segoe UI';"
                          + "-fx-padding: 8px 0 4px 0;");

        // ── Price ────────────────────────────────────────────
        Label priceLabel = new Label(price);
        priceLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;"
                          + "-fx-text-fill: #E53238; -fx-font-family: 'Segoe UI';");

        // ── Bids + badge row ─────────────────────────────────
        Label bidsLabel = new Label(bids);
        bidsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;"
                         + "-fx-font-family: 'Segoe UI';");

        Label badge = new Label(timeLeft);
        String badgeStyle = "-fx-font-size: 10px; -fx-font-weight: bold;"
                          + "-fx-padding: 2px 8px;"
                          + "-fx-background-radius: 10px;";
        if ("success".equals(badgeType)) {
            badge.setStyle(badgeStyle
                + "-fx-background-color: #EAF5EA; -fx-text-fill: #5BA55B;");
        } else {
            badge.setStyle(badgeStyle
                + "-fx-background-color: #FEF6E6; -fx-text-fill: #F5A623;");
        }

        HBox metaRow = new HBox(bidsLabel, badge);
        metaRow.setSpacing(0);
        metaRow.setStyle("-fx-alignment: CENTER_LEFT; -fx-spacing: 0;");
        // Push badge to right side
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        metaRow.getChildren().add(1, spacer);

        // ── Assemble card ────────────────────────────────────
        VBox card = new VBox(imgBox, titleLabel, priceLabel, metaRow);
        card.setPrefWidth(160);
        card.setStyle("-fx-background-color: white;"
                    + "-fx-border-color: #EBEBEB;"
                    + "-fx-border-width: 1px;"
                    + "-fx-border-radius: 8px;"
                    + "-fx-background-radius: 8px;"
                    + "-fx-padding: 10px;"
                    + "-fx-cursor: hand;");

        // Hover effect — border turns red
        card.setOnMouseEntered(e ->
            card.setStyle("-fx-background-color: white;"
                        + "-fx-border-color: #E53238;"
                        + "-fx-border-width: 1.5px;"
                        + "-fx-border-radius: 8px;"
                        + "-fx-background-radius: 8px;"
                        + "-fx-padding: 10px;"
                        + "-fx-cursor: hand;"
                        + "-fx-scale-x: 1.02;"
                        + "-fx-scale-y: 1.02;")
        );
        card.setOnMouseExited(e ->
            card.setStyle("-fx-background-color: white;"
                        + "-fx-border-color: #EBEBEB;"
                        + "-fx-border-width: 1px;"
                        + "-fx-border-radius: 8px;"
                        + "-fx-background-radius: 8px;"
                        + "-fx-padding: 10px;"
                        + "-fx-cursor: hand;")
        );

        // Click → TODO: navigate to AuctionDetailView
        card.setOnMouseClicked(e ->
            System.out.println("TODO: open detail for → " + title)
        );

        return card;
    }

    // ── State helpers ─────────────────────────────────────────

    private void showEmpty(boolean show) {
        emptyState.setVisible(show);
        emptyState.setManaged(show);
        endingSoonGrid.setVisible(!show);
        endingSoonGrid.setManaged(!show);
        recentGrid.setVisible(!show);
        recentGrid.setManaged(!show);
    }
}