package com.auction.client.controllers;

import com.auction.client.services.NetworkClientService;
import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.UserSession;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
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
 * Fetches the seller's listings from the server and displays them live.
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
  private final Gson gson = new Gson();

  /** All listings fetched from the server (cached for filtering). */
  private final List<JsonObject> allListings = new ArrayList<>();
  private String currentFilter = null; // null = all

  @Override
  public void initialize(final URL url, final ResourceBundle rb) {
    userLabel.setText(UserSession.getInstance().getInitials());
    activeFilter = filterAll;
    fetchListingsFromServer();
  }

  /**
   * Sends GET_SELLER_ITEMS to the server and populates the table when
   * the response arrives.
   */
  private void fetchListingsFromServer() {
    listingsContainer.getChildren().clear();
    listingCountLabel.setText("Loading...");

    final Request req = new Request(MessageType.GET_SELLER_ITEMS,
        UserSession.getInstance().getUserId(), "");

    // One-shot listener
    final NetworkClientService.ServerMessageListener[] ref =
        new NetworkClientService.ServerMessageListener[1];
    ref[0] = response -> {
      if (response.getType() == MessageType.GET_SELLER_ITEMS_RESPONSE) {
        NetworkClientService.getInstance().removeListener(ref[0]);

        // Parse the JSON array from the payload
        final List<JsonObject> items = new ArrayList<>();
        try {
          final JsonArray arr = gson.fromJson(response.getPayload(), JsonArray.class);
          if (arr != null) {
            for (final JsonElement el : arr) {
              items.add(el.getAsJsonObject());
            }
          }
        } catch (Exception ignored) { }

        Platform.runLater(() -> {
          allListings.clear();
          allListings.addAll(items);
          applyFilter();
        });
      }
    };
    NetworkClientService.getInstance().addListener(ref[0]);
    NetworkClientService.getInstance().sendRequest(req);
  }

  // ── Filters ─────────────────────────────────────────────────

  @FXML
  private void onFilterAll() {
    switchFilter(filterAll);
    currentFilter = null;
    applyFilter();
  }

  @FXML
  private void onFilterActive() {
    switchFilter(filterActive);
    currentFilter = "OPEN";
    applyFilter();
  }

  @FXML
  private void onFilterSold() {
    switchFilter(filterSold);
    currentFilter = "FINISHED";
    applyFilter();
  }

  @FXML
  private void onFilterUnsold() {
    switchFilter(filterUnsold);
    currentFilter = "CANCELED";
    applyFilter();
  }

  @FXML
  private void onFilterDraft() {
    switchFilter(filterDraft);
    currentFilter = "DRAFT";
    applyFilter();
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

  private void applyFilter() {
    final List<JsonObject> filtered;
    if (currentFilter == null) {
      filtered = allListings;
    } else {
      filtered = new ArrayList<>();
      for (final JsonObject item : allListings) {
        if (item.has("status") && currentFilter.equals(item.get("status").getAsString())) {
          filtered.add(item);
        }
      }
    }
    populateRows(filtered);
  }

  // ── Row rendering ───────────────────────────────────────────

  private void populateRows(final List<JsonObject> data) {
    listingsContainer.getChildren().clear();
    if (data.isEmpty()) {
      emptyState.setVisible(true);
      emptyState.setManaged(true);
      listingCountLabel.setText("0 listings");
      return;
    }
    emptyState.setVisible(false);
    emptyState.setManaged(false);
    listingCountLabel.setText(data.size() + " listing" + (data.size() == 1 ? "" : "s"));
    for (final JsonObject item : data) {
      listingsContainer.getChildren().add(buildRow(item));
    }
  }

  private HBox buildRow(final JsonObject item) {
    final String name = item.has("name") ? item.get("name").getAsString() : "Untitled";
    final double currentPrice = item.has("currentPrice") ? item.get("currentPrice").getAsDouble() : 0;
    final double startPrice = item.has("startPrice") ? item.get("startPrice").getAsDouble() : 0;
    final String status = item.has("status") ? item.get("status").getAsString() : "OPEN";
    final String itemId = item.has("id") ? item.get("id").getAsString() : "";
    final String description = item.has("description") ? item.get("description").getAsString() : "";
    final String category = item.has("category") ? item.get("category").getAsString() : "";

    final HBox row = new HBox();
    row.setAlignment(Pos.CENTER_LEFT);
    row.setStyle("-fx-border-color:transparent transparent #F4F4F4 transparent;"
        + "-fx-border-width:0 0 1px 0;-fx-padding:10px 14px;");

    final Label titleLbl = new Label(name);
    titleLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#111;");
    HBox.setHgrow(titleLbl, Priority.ALWAYS);

    final Label currentBidLbl = new Label(String.format("$%.2f", currentPrice));
    currentBidLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#E53238;");
    currentBidLbl.setPrefWidth(100);

    final Label startPriceLbl = new Label(String.format("$%.2f", startPrice));
    startPriceLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#333;");
    startPriceLbl.setPrefWidth(120);

    final Label timeLbl = new Label(formatTimeLeft(item));
    timeLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#888;");
    timeLbl.setPrefWidth(100);

    final Label badge = buildBadge(status);
    badge.setPrefWidth(90);

    final Button action = buildActionButton(status, itemId, name, category, description);
    action.setPrefWidth(100);

    row.getChildren().addAll(titleLbl, currentBidLbl, startPriceLbl, timeLbl, badge, action);
    return row;
  }

  private String formatTimeLeft(final JsonObject item) {
    if (!item.has("endTime")) return "-";
    final long endTime = item.get("endTime").getAsLong();
    final long now = System.currentTimeMillis();
    final long diff = endTime - now;
    if (diff <= 0) return "Ended";
    final long hours = diff / (3600 * 1000);
    final long days = hours / 24;
    if (days > 0) return days + "d " + (hours % 24) + "h";
    final long minutes = (diff % (3600 * 1000)) / (60 * 1000);
    return hours + "h " + minutes + "m";
  }

  private Label buildBadge(final String status) {
    final Label b = new Label();
    switch (status) {
      case "OPEN" -> {
        b.setText("Active");
        b.setStyle(badgeStyle("#E8F0FE", "#1A73E8"));
      }
      case "FINISHED" -> {
        b.setText("Sold");
        b.setStyle(badgeStyle("#EAF5EA", "#5BA55B"));
      }
      case "CANCELED" -> {
        b.setText("Unsold");
        b.setStyle(badgeStyle("#FEF6E6", "#F5A623"));
      }
      case "DRAFT" -> {
        b.setText("Draft");
        b.setStyle(badgeStyle("#F4F4F4", "#888888"));
      }
      default -> {
        b.setText(status);
        b.setStyle(badgeStyle("#F4F4F4", "#333333"));
      }
    }
    return b;
  }

  private String badgeStyle(final String bg, final String fg) {
    return "-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";"
        + "-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:3px 10px;"
        + "-fx-background-radius:10px;";
  }

  private Button buildActionButton(final String status, final String itemId,
      final String name, final String category, final String description) {
    final Button btn = new Button();
    btn.setStyle("-fx-background-color:white;-fx-border-color:#E0E0E0;"
        + "-fx-border-width:1px;-fx-border-radius:5px;"
        + "-fx-background-radius:5px;-fx-font-size:11px;"
        + "-fx-padding:4px 12px;-fx-cursor:hand;-fx-effect:null;");

    btn.setText("View");
    btn.setOnAction(e -> {
      UserSession.getInstance().setSelectedItemId(itemId);
      UserSession.getInstance().setSelectedAuctionTitle(name);
      UserSession.getInstance().setSelectedAuctionCategory(category);
      UserSession.getInstance().setSelectedItemDescription(description);
      SceneNavigator.navigateTo(SceneNavigator.View.AUCTION_DETAIL);
    });
    return btn;
  }

  // ── Navigation ──────────────────────────────────────────────

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

  @FXML
  private void onToggleTheme() {
    SceneNavigator.toggleTheme();
  }
}
