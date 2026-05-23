package com.auction.client.controllers;

import com.auction.client.services.NetworkClientService;
import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.UserSession;
import com.auction.client.utils.TopNavUtils;
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
    row.getStyleClass().add("table-row");

    final Label titleLbl = new Label(name);
    titleLbl.getStyleClass().add("label-bold");
    HBox.setHgrow(titleLbl, Priority.ALWAYS);
    titleLbl.setMaxWidth(Double.MAX_VALUE);

    final Label currentBidLbl = new Label(String.format("$%.2f", currentPrice));
    currentBidLbl.getStyleClass().addAll("label-bold", "price-tag");
    currentBidLbl.setPrefWidth(120);
    currentBidLbl.setStyle("-fx-font-size: 14px;");

    final Label startPriceLbl = new Label(String.format("$%.2f", startPrice));
    startPriceLbl.getStyleClass().add("body-text");
    startPriceLbl.setPrefWidth(140);

    final Label timeLbl = new Label(formatTimeLeft(item));
    timeLbl.getStyleClass().add("body-small");
    timeLbl.setPrefWidth(120);

    final Label badge = buildBadge(status);
    badge.setPrefWidth(100);

    final long endTime = item.has("endTime") ? item.get("endTime").getAsLong() : 0L;
    final String base64Image = item.has("imageData") && !item.get("imageData").isJsonNull() ? item.get("imageData").getAsString() : "";
    byte[] imgBytes = null;
    if (!base64Image.isEmpty()) {
      try {
        imgBytes = java.util.Base64.getDecoder().decode(base64Image);
      } catch (IllegalArgumentException ignored) {}
    }

    final Button action = buildActionButton(status, itemId, name, category, description, currentPrice, startPrice, endTime, imgBytes);
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
    b.getStyleClass().add("badge");
    switch (status) {
      case "OPEN" -> {
        b.setText("Active");
        b.getStyleClass().add("badge-info");
      }
      case "FINISHED" -> {
        b.setText("Sold");
        b.getStyleClass().add("badge-success");
      }
      case "CANCELED" -> {
        b.setText("Unsold");
        b.getStyleClass().add("badge-warning");
      }
      case "DRAFT" -> {
        b.setText("Draft");
        b.getStyleClass().add("badge-danger");
      }
      default -> {
        b.setText(status);
        b.getStyleClass().add("badge-danger");
      }
    }
    return b;
  }


  private Button buildActionButton(final String status, final String itemId,
      final String name, final String category, final String description,
      final double currentPrice, final double startPrice, final long endTime,
      final byte[] imgBytes) {
    final Button btn = new Button();
    btn.getStyleClass().add("btn-outline");
    btn.setStyle("-fx-font-size: 11px; -fx-padding: 4px 12px;");

    btn.setText("View");
    btn.setOnAction(e -> {
      UserSession.getInstance().setSelectedItemId(itemId);
      UserSession.getInstance().setSelectedAuctionTitle(name);
      UserSession.getInstance().setSelectedAuctionCategory(category);
      UserSession.getInstance().setSelectedItemDescription(description);
      UserSession.getInstance().setSelectedItemPrice(currentPrice > 0 ? currentPrice : startPrice);
      UserSession.getInstance().setSelectedItemEndTime(endTime);
      UserSession.getInstance().setSelectedItemImageData(imgBytes);
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
  private void onLogout() {
    UserSession.getInstance().clear();
    SceneNavigator.navigateTo(SceneNavigator.View.LOGIN);
  }
}
