package com.auction.client.controllers;

import com.auction.client.services.NetworkClientService;
import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.UserSession;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.client.utils.ItemDeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class HomeController implements Initializable {
  private static final Logger LOGGER = Logger.getLogger(HomeController.class.getName());


  private static final double SIDEBAR_WIDTH = 190;
  private static final double SCROLL_PADDING = 54;
  private static final double TARGET_CARD_WIDTH = 200;
  private static final double CARD_MIN_WIDTH = 150;
  private static final double CARD_MAX_WIDTH = 280;
  private static final int MIN_COLUMNS = 2;
  private static final double CARD_GAP = 14;
  private static final double IMAGE_RATIO = 0.65;

  @FXML private BorderPane rootPane;
  @FXML private TextField searchField;
  @FXML private Label userLabel;
  @FXML private Button allCategoriesBtn;
  @FXML private Button electronicsBtn;
  @FXML private Button fashionBtn;
  @FXML private Button adminBtn;
  @FXML private Button homeGardenBtn;
  @FXML private Button sportsBtn;
  @FXML private Button collectiblesBtn;
  @FXML private Button vehiclesBtn;
  @FXML private Button otherBtn;
  @FXML private FlowPane endingSoonGrid;
  @FXML private FlowPane recentGrid;
  @FXML private VBox emptyState;
  @FXML private VBox loadingState;

  private Button activeCategory;
  private String currentCategory;
  private String currentSearch = "";

  private final Map<String, Label> priceLabels = new HashMap<>();
  private final java.util.List<com.auction.shared.models.Item> allAuctionItems = new java.util.ArrayList<>();
  private final Gson itemGson = new GsonBuilder()
      .registerTypeAdapter(com.auction.shared.models.Item.class, new ItemDeserializer())
      .create();

  @Override
  public void initialize(final URL url, final ResourceBundle rb) {
    activeCategory = allCategoriesBtn;
    userLabel.setText(UserSession.getInstance().getInitials());
    
    if (adminBtn != null) {
      boolean isAdmin = UserSession.getInstance().isAdmin();
      adminBtn.setVisible(isAdmin);
      adminBtn.setManaged(isAdmin);
    }
    
    NetworkClientService.getInstance().addListener(this::handleServerMessage);
    fetchItemsFromServer();

    rootPane.widthProperty().addListener((obs, oldW, newW) -> onWindowResized(newW.doubleValue()));
  }

  private void handleServerMessage(Response response) {
    if (response.getType() == MessageType.NEW_ITEM_BROADCAST) {
      Platform.runLater(this::fetchItemsFromServer);
    } else if (response.getType() == MessageType.NEW_BID_BROADCAST) {
      try {
        final com.auction.shared.models.BidTransaction tx = new Gson().fromJson(response.getPayload(), com.auction.shared.models.BidTransaction.class);
        if (tx == null || tx.getItemId() == null) return;
        Platform.runLater(() -> {
          final Label lbl = priceLabels.get(tx.getItemId());
          if (lbl != null) lbl.setText(String.format("$%.2f", tx.getBidAmount()));
        });
      } catch (Exception ignored) { }
    }
  }

  private void fetchItemsFromServer() {
    final Request req = new Request(MessageType.GET_ALL_ITEMS, UserSession.getInstance().getUserId(), "");
    final NetworkClientService.ServerMessageListener[] ref = new NetworkClientService.ServerMessageListener[1];
    ref[0] = response -> {
      if (response.getType() == MessageType.GET_ALL_ITEMS_RESPONSE) {
        NetworkClientService.getInstance().removeListener(ref[0]);
        try {
          final com.auction.shared.models.Item[] items = itemGson.fromJson(response.getPayload(), com.auction.shared.models.Item[].class);
          Platform.runLater(() -> {
            allAuctionItems.clear();
            if (items != null) allAuctionItems.addAll(Arrays.asList(items));
            applyFilters();
          });
        } catch (Exception e) {
          LOGGER.log(Level.SEVERE, "Error parsing items from server", e);
        }
      }
    };
    NetworkClientService.getInstance().addListener(ref[0]);
    NetworkClientService.getInstance().sendRequest(req);
  }

  private void onWindowResized(final double windowWidth) {
    final double available = windowWidth - SIDEBAR_WIDTH - SCROLL_PADDING;
    final int numCols = Math.max(MIN_COLUMNS, (int) (available / TARGET_CARD_WIDTH));
    double cardWidth = (available / numCols) - CARD_GAP;
    cardWidth = Math.max(CARD_MIN_WIDTH, Math.min(cardWidth, CARD_MAX_WIDTH));
    updateCardWidths(endingSoonGrid, cardWidth);
    updateCardWidths(recentGrid, cardWidth);
  }

  private void updateCardWidths(final FlowPane grid, final double cardWidth) {
    grid.getChildren().forEach(node -> {
      if (node instanceof VBox) {
        VBox card = (VBox) node;
        card.setPrefWidth(cardWidth);
        if (!card.getChildren().isEmpty() && card.getChildren().get(0) instanceof StackPane) {
          StackPane imgBox = (StackPane) card.getChildren().get(0);
          imgBox.setPrefWidth(cardWidth - 20);
          imgBox.setPrefHeight((cardWidth - 20) * IMAGE_RATIO);
        }
      }
    });
  }

  @FXML private void onSearch() { currentSearch = searchField.getText().trim().toLowerCase(); applyFilters(); }
  @FXML private void onClearSearch() { searchField.clear(); currentSearch = ""; applyFilters(); }
  @FXML private void onCategoryAll() { switchCategory(allCategoriesBtn, null); }
  @FXML private void onCategoryElectronics() { switchCategory(electronicsBtn, "ELECTRONICS"); }
  @FXML private void onCategoryFashion() { switchCategory(fashionBtn, "FASHION"); }
  @FXML private void onCategoryHome() { switchCategory(homeGardenBtn, "HOME_AND_GARDEN"); }
  @FXML private void onCategorySports() { switchCategory(sportsBtn, "SPORTS"); }
  @FXML private void onCategoryCollectibles() { switchCategory(collectiblesBtn, "COLLECTIBLES"); }
  @FXML private void onCategoryVehicles() { switchCategory(vehiclesBtn, "VEHICLE"); }
  @FXML private void onCategoryOther() { switchCategory(otherBtn, "OTHER"); }

  private void switchCategory(final Button btn, final String category) {
    if (activeCategory != null) {
      activeCategory.getStyleClass().remove("nav-item-active");
      activeCategory.getStyleClass().add("nav-item");
    }
    btn.getStyleClass().remove("nav-item");
    btn.getStyleClass().add("nav-item-active");
    activeCategory = btn;
    currentCategory = category;
    applyFilters();
  }

  private void applyFilters() {
    final java.util.List<com.auction.shared.models.Item> filtered = allAuctionItems.stream()
        .filter(item -> currentCategory == null || item.getCategory().name().equals(currentCategory))
        .filter(item -> currentSearch.isEmpty() || item.getName().toLowerCase().contains(currentSearch))
        .toList();

    if (filtered.isEmpty()) {
      showEmpty(true);
    } else {
      showEmpty(false);
      populateGrid(endingSoonGrid, filtered); // Simulating ending soon
      populateGrid(recentGrid, filtered);     // Simulating recent
      if (rootPane.getWidth() > 0) onWindowResized(rootPane.getWidth());
    }
  }

  private void populateGrid(final FlowPane grid, final java.util.List<com.auction.shared.models.Item> data) {
    grid.getChildren().clear();
    for (final com.auction.shared.models.Item item : data) grid.getChildren().add(buildCard(item));
  }

  private VBox buildCard(final com.auction.shared.models.Item item) {
    final String itemId = item.getId();
    final String title = item.getName();
    final String price = String.format("$%.2f", item.getCurrentHighestBid());
    final String category = item.getCategory().name();
    
    final StackPane imgBox = new StackPane(new Label("No image"));
    imgBox.getStyleClass().add("card-img-placeholder");

    final Label catChip = new Label(category);
    catChip.getStyleClass().addAll("badge", "badge-info");

    final Label titleLabel = new Label(title);
    titleLabel.setWrapText(true);
    titleLabel.getStyleClass().add("label-bold");

    final Label priceLabel = new Label(price);
    priceLabel.getStyleClass().add("price-tag");
    priceLabels.put(itemId, priceLabel);

    final long remaining = item.getEndTime() - System.currentTimeMillis();
    final Label badge = new Label(remaining > 0 ? (remaining / 3600000) + "h left" : "Ended");
    badge.getStyleClass().add("badge");
    if (remaining > 3600000) badge.getStyleClass().add("badge-success");
    else if (remaining > 0) badge.getStyleClass().add("badge-warning");
    else badge.getStyleClass().add("badge-danger");

    final HBox metaRow = new HBox(new Label("Live"), new Region(), badge);
    HBox.setHgrow(metaRow.getChildren().get(1), Priority.ALWAYS);
    metaRow.setAlignment(Pos.CENTER_LEFT);

    final VBox card = new VBox(8, imgBox, catChip, titleLabel, priceLabel, metaRow);
    card.getStyleClass().addAll("auction-card", "card-hover");
    card.setOnMouseClicked(e -> {
      UserSession.getInstance().setSelectedAuctionTitle(title);
      UserSession.getInstance().setSelectedAuctionCategory(category);
      UserSession.getInstance().setSelectedItemId(itemId);
      UserSession.getInstance().setSelectedItemDescription(item.getDescription());
      SceneNavigator.navigateTo(SceneNavigator.View.AUCTION_DETAIL);
    });
    return card;
  }

  private void showEmpty(final boolean show) {
    emptyState.setVisible(show); emptyState.setManaged(show);
    endingSoonGrid.setVisible(!show); endingSoonGrid.setManaged(!show);
    recentGrid.setVisible(!show); recentGrid.setManaged(!show);
  }

  @FXML private void onSeeAllEndingSoon() { /* placeholder — scroll to section or navigate */ }
  @FXML private void onSeeAllRecent() { /* placeholder — scroll to section or navigate */ }
  @FXML private void onSell() { SceneNavigator.navigateTo(SceneNavigator.View.SELLER); }
  @FXML private void onMyBids() { SceneNavigator.navigateTo(SceneNavigator.View.MY_BIDS); }
  @FXML private void onWatchlist() { SceneNavigator.navigateTo(SceneNavigator.View.MY_BIDS); }
  @FXML private void onProfile() { SceneNavigator.navigateTo(SceneNavigator.View.PROFILE); }
  @FXML private void onLogout() { UserSession.getInstance().clear(); SceneNavigator.navigateTo(SceneNavigator.View.LOGIN); }
  @FXML private void onToggleTheme() { SceneNavigator.toggleTheme(); }
  @FXML private void onHome() { SceneNavigator.navigateTo(SceneNavigator.View.HOME); }
  @FXML private void onAdmin() { SceneNavigator.navigateTo(SceneNavigator.View.ADMIN); }
}