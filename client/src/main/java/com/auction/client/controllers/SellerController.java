package com.auction.client.controllers;

import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.UserSession;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * SellerController handles the Seller Dashboard view.
 * Displays listings created by the seller and their status.
 */
public class SellerController implements Initializable {

  @FXML private Label userLabel;
  @FXML private Label listingCountLabel;
  @FXML private VBox listingsContainer;
  @FXML private VBox emptyState;
  @FXML private Button filterAll;
  @FXML private Button filterActive;
  @FXML private Button filterSold;
  @FXML private Button filterUnsold;
  @FXML private Button filterDraft;

  private Button activeFilter;

  // Mock data for listings
  private static final String[][] ALL_LISTINGS = {
    {"Vintage Rolex Watch", "$1,240.00", "$1,500.00", "1h 47m", "active"},
    {"iPhone 15 Pro Max", "$780.00", "$900.00", "Ended", "sold"},
    {"Nike Air Jordan 1", "$0.00", "$250.00", "Ended", "unsold"},
    {"Sony WH-1000XM5", "-", "$200.00", "-", "draft"},
    {"MacBook Air M2", "$820.00", "$1,000.00", "4d 2h", "active"}
  };

  @Override
  public void initialize(final URL url, final ResourceBundle rb) {
    userLabel.setText(UserSession.getInstance().getInitials());
    activeFilter = filterAll;
    populateRows(ALL_LISTINGS);
  }

  @FXML
  private void onFilterAll() {
    switchFilter(filterAll);
    populateRows(ALL_LISTINGS);
  }

  @FXML
  private void onFilterActive() {
    switchFilter(filterActive);
    populateRows(filterBy("active"));
  }

  @FXML
  private void onFilterSold() {
    switchFilter(filterSold);
    populateRows(filterBy("sold"));
  }

  @FXML
  private void onFilterUnsold() {
    switchFilter(filterUnsold);
    populateRows(filterBy("unsold"));
  }

  @FXML
  private void onFilterDraft() {
    switchFilter(filterDraft);
    populateRows(filterBy("draft"));
  }

  private void switchFilter(final Button btn) {
    if (activeFilter != null) {
      activeFilter.getStyleClass().remove("nav-item-active");
      activeFilter.getStyleClass().add("nav-item");
    }
    btn.getStyleClass().remove("nav-item");
    btn.getStyleClass().add("nav-item-active");
    activeFilter = btn;
  }

  private String[][] filterBy(final String status) {
    return Arrays.stream(ALL_LISTINGS)
        .filter(b -> b[4].equals(status))
        .toArray(String[][]::new);
  }

  @FXML
  private void onHome() {
    SceneNavigator.navigateTo(SceneNavigator.View.HOME);
  }

  @FXML
  private void onCreateListing() {
    SceneNavigator.navigateTo(SceneNavigator.View.CREATE_LISTING);
  }

  @FXML
  private void onProfile() {
    SceneNavigator.navigateTo(SceneNavigator.View.PROFILE);
  }

  @FXML
  private void onLogout() {
    UserSession.getInstance().clear();
    SceneNavigator.navigateTo(SceneNavigator.View.LOGIN);
  }

  private void populateRows(final String[][] data) {
    listingsContainer.getChildren().clear();
    if (data.length == 0) {
      emptyState.setVisible(true);
      emptyState.setManaged(true);
      listingCountLabel.setText("0 listings");
      return;
    }
    emptyState.setVisible(false);
    emptyState.setManaged(false);
    listingCountLabel.setText(data.length + " listing" + (data.length == 1 ? "" : "s"));
    for (final String[] listing : data) {
      listingsContainer.getChildren().add(
          buildRow(listing[0], listing[1], listing[2], listing[3], listing[4]));
    }
  }

  private HBox buildRow(final String title, final String currentBid,
      final String buyNowPrice, final String timeLeft, final String status) {
    final HBox row = new HBox();
    row.setAlignment(Pos.CENTER_LEFT);
    row.setStyle("-fx-border-color:transparent transparent #F4F4F4 transparent;"
        + "-fx-border-width:0 0 1px 0;-fx-padding:10px 14px;");

    final Label titleLbl = new Label(title);
    titleLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#111;");
    HBox.setHgrow(titleLbl, Priority.ALWAYS);

    final Label currentBidLbl = new Label(currentBid);
    currentBidLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#E53238;");
    currentBidLbl.setPrefWidth(100);

    final Label buyNowPriceLbl = new Label(buyNowPrice);
    buyNowPriceLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#333;");
    buyNowPriceLbl.setPrefWidth(120);

    final Label timeLbl = new Label(timeLeft);
    timeLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#888;");
    timeLbl.setPrefWidth(100);

    final Label badge = buildBadge(status);
    badge.setPrefWidth(90);
    final Button action = buildActionButton(status);
    action.setPrefWidth(100);

    row.getChildren().addAll(titleLbl, currentBidLbl, buyNowPriceLbl, timeLbl, badge, action);
    return row;
  }

  private Label buildBadge(final String status) {
    final Label b = new Label();
    switch (status) {
      case "active" -> {
        b.setText("Active");
        b.setStyle(badgeStyle("#E8F0FE", "#1A73E8"));
      }
      case "sold" -> {
        b.setText("Sold");
        b.setStyle(badgeStyle("#EAF5EA", "#5BA55B"));
      }
      case "unsold" -> {
        b.setText("Unsold");
        b.setStyle(badgeStyle("#FEF6E6", "#F5A623"));
      }
      case "draft" -> {
        b.setText("Draft");
        b.setStyle(badgeStyle("#F4F4F4", "#888888"));
      }
      default -> b.setText(status);
    }
    return b;
  }

  private String badgeStyle(final String bg, final String fg) {
    return "-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";"
        + "-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:3px 10px;"
        + "-fx-background-radius:10px;";
  }

  private Button buildActionButton(final String status) {
    final Button btn = new Button();
    btn.setStyle("-fx-background-color:white;-fx-border-color:#E0E0E0;"
        + "-fx-border-width:1px;-fx-border-radius:5px;"
        + "-fx-background-radius:5px;-fx-font-size:11px;"
        + "-fx-padding:4px 12px;-fx-cursor:hand;-fx-effect:null;");

    switch (status) {
      case "draft" -> {
        btn.setText("Edit");
        btn.setOnAction(e -> SceneNavigator.navigateTo(SceneNavigator.View.CREATE_LISTING));
      }
      case "sold" -> {
        btn.setText("Ship Item");
        btn.setStyle("-fx-background-color:#E53238;-fx-text-fill:white;"
            + "-fx-font-weight:bold;-fx-border-color:transparent;"
            + "-fx-border-radius:5px;-fx-background-radius:5px;"
            + "-fx-font-size:11px;-fx-padding:4px 12px;-fx-cursor:hand;");
        btn.setOnAction(e -> System.out.println("TODO: shipping flow"));
      }
      default -> {
        btn.setText("View");
        btn.setOnAction(e -> SceneNavigator.navigateTo(SceneNavigator.View.AUCTION_DETAIL));
      }
    }
    return btn;
  }
}
