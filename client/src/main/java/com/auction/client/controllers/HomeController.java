package com.auction.client.controllers;

import com.auction.client.services.NetworkClientService;
import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.UserSession;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.google.gson.Gson;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
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

/**
 * HomeController — handles HomeView.fxml.
 *
 * <p>Features:
 * <ul>
 * <li>Responsive card layout — cards resize when the window is resized.</li>
 * <li>Real category filter — clicking a sidebar button filters grids.</li>
 * <li>Combined search + category filter.</li>
 * </ul>
 */
public class HomeController implements Initializable {

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
  @FXML private Button homeGardenBtn;
  @FXML private Button sportsBtn;
  @FXML private Button collectiblesBtn;
  @FXML private Button vehiclesBtn;
  @FXML private Button otherBtn;
  @FXML private FlowPane endingSoonGrid;
  @FXML private FlowPane recentGrid;
  @FXML private VBox emptyState;
  @FXML private VBox loadingState;
  @FXML private Label seeAllEndingSoon;
  @FXML private Label seeAllRecent;

  private Button activeCategory;
  private String currentCategory;
  private String currentSearch = "";

  /** Maps itemId → the price Label in its card, so we can update it live. */
  private final Map<String, Label> priceLabels = new HashMap<>();

  // Columns: [itemId, title, price, bids, timeLeft, badgeType, category, section, description]
  private static final String[][] ALL_AUCTIONS = {
    {"ITEM-001", "Gaming Laptop RTX 4080", "$1,240.00", "0 bids", "30d left",
        "warning", "Electronics", "ending", "Asus ROG Strix with RTX 4080, i9, 32GB DDR5"},
    {"ITEM-002", "iPhone 15 Pro Max 256GB", "$780.00", "0 bids", "1d left",
        "warning", "Electronics", "ending", "Brand new iPhone 15 Pro Max, Natural Titanium"},
    {"ITEM-006", "Vintage Rolex Submariner 1969", "$4,500.00", "0 bids", "3d left",
        "warning", "Collectibles", "ending", "Original 1969 Rolex Submariner. Serviced 2022."},
    {"ITEM-003", "Sony WH-1000XM5", "$190.00", "0 bids", "2d left",
        "warning", "Electronics", "ending", "Industry-leading noise cancelling headphones"},
    {"ITEM-004", "MacBook Air M2 13\"", "$850.00", "0 bids", "5d left",
        "warning", "Electronics", "recent", "Apple M2 chip, 8GB RAM, 256GB SSD, Midnight"},
    {"ITEM-005", "Mountain Bike 2024 Carbon", "$320.00", "0 bids", "7d left",
        "warning", "Sports", "recent", "29\" Carbon frame, Shimano 12-speed drivetrain"},
  };

  @Override
  public void initialize(final URL url, final ResourceBundle rb) {
    activeCategory = allCategoriesBtn;
    userLabel.setText(UserSession.getInstance().getInitials());
    applyFilters();

    rootPane.widthProperty().addListener(
            (obs, oldW, newW) -> onWindowResized(newW.doubleValue()));

    // Fetch live prices for all visible items once the scene is ready
    refreshLivePrices();
  }

  /**
   * Sends a GET_STATUS request for each item in ALL_AUCTIONS and updates
   * the matching price label when the server responds.
   */
  private void refreshLivePrices() {
    final Gson gson = new Gson();
    final String userId = UserSession.getInstance().getUserId();
    final String[] itemIds = Arrays.stream(ALL_AUCTIONS)
        .map(row -> row[0]).toArray(String[]::new);

    // One shared listener — handles all incoming NEW_BID_BROADCAST status replies
    final NetworkClientService.ServerMessageListener[] ref =
        new NetworkClientService.ServerMessageListener[1];
    final int[] remaining = {itemIds.length};

    ref[0] = response -> {
      if (response.getType() != MessageType.NEW_BID_BROADCAST) return;
      try {
        final com.auction.shared.models.BidTransaction tx =
            gson.fromJson(response.getPayload(), com.auction.shared.models.BidTransaction.class);
        if (tx == null || tx.getItemId() == null) return;

        final String formattedPrice = String.format("$%.2f", tx.getBidAmount());
        Platform.runLater(() -> {
          final Label lbl = priceLabels.get(tx.getItemId());
          if (lbl != null) {
            lbl.setText(formattedPrice);
          }
        });
      } catch (Exception ignored) { }

      synchronized (remaining) {
        remaining[0]--;
        if (remaining[0] <= 0) {
          NetworkClientService.getInstance().removeListener(ref[0]);
        }
      }
    };

    NetworkClientService.getInstance().addListener(ref[0]);

    // Fire one GET_STATUS per item with a small delay between each so the
    // server isn't flooded and responses stay distinguishable
    new Thread(() -> {
      for (final String itemId : itemIds) {
        final Request req = new Request(MessageType.GET_STATUS, userId, itemId);
        NetworkClientService.getInstance().sendRequest(req);
        try { Thread.sleep(80); } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }, "HomePrice-Refresh").start();
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
      if (node instanceof VBox card) {
        card.setPrefWidth(cardWidth);
        if (!card.getChildren().isEmpty()
            && card.getChildren().get(0) instanceof StackPane imgBox) {
          imgBox.setPrefWidth(cardWidth - 20);
          imgBox.setPrefHeight((cardWidth - 20) * IMAGE_RATIO);
        }
        card.getChildren().stream()
            .filter(n -> n instanceof Label)
            .map(n -> (Label) n)
            .findFirst()
            .ifPresent(lbl -> lbl.setMaxWidth(cardWidth - 20));
      }
    });
  }

  @FXML
  private void onSearch() {
    currentSearch = searchField.getText().trim().toLowerCase();
    applyFilters();
  }

  @FXML
  private void onClearSearch() {
    searchField.clear();
    currentSearch = "";
    applyFilters();
  }

  @FXML
  private void onCategoryAll() {
    switchCategory(allCategoriesBtn, null);
  }

  @FXML
  private void onCategoryElectronics() {
    switchCategory(electronicsBtn, "Electronics");
  }

  @FXML
  private void onCategoryFashion() {
    switchCategory(fashionBtn, "Fashion");
  }

  @FXML
  private void onCategoryHome() {
    switchCategory(homeGardenBtn, "Home & Garden");
  }

  @FXML
  private void onCategorySports() {
    switchCategory(sportsBtn, "Sports");
  }

  @FXML
  private void onCategoryCollectibles() {
    switchCategory(collectiblesBtn, "Collectibles");
  }

  @FXML
  private void onCategoryVehicles() {
    switchCategory(vehiclesBtn, "Vehicles");
  }

  @FXML
  private void onCategoryOther() {
    switchCategory(otherBtn, "Other");
  }

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
    final String[][] filtered = Arrays.stream(ALL_AUCTIONS)
        .filter(item -> currentCategory == null || item[6].equals(currentCategory))
        .filter(item -> currentSearch.isEmpty()
            || item[1].toLowerCase().contains(currentSearch))
        .toArray(String[][]::new);

    final String[][] ending = Arrays.stream(filtered)
        .filter(item -> "ending".equals(item[7]))
        .toArray(String[][]::new);

    final String[][] recent = Arrays.stream(filtered)
        .filter(item -> "recent".equals(item[7]))
        .toArray(String[][]::new);

    if (filtered.length == 0) {
      showEmpty(true);
    } else {
      showEmpty(false);
      populateGrid(endingSoonGrid, ending);
      populateGrid(recentGrid, recent);
      if (rootPane.getWidth() > 0) {
        onWindowResized(rootPane.getWidth());
      }
    }
  }

  @FXML
  private void onSell() {
    SceneNavigator.navigateTo(SceneNavigator.View.SELLER);
  }

  @FXML
  private void onWatchlist() {
    SceneNavigator.navigateTo(SceneNavigator.View.MY_BIDS);
  }

  @FXML
  private void onMyBids() {
    SceneNavigator.navigateTo(SceneNavigator.View.MY_BIDS);
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
  private void onSeeAllEndingSoon() {
    System.out.println("See all ending soon");
  }

  @FXML
  private void onSeeAllRecent() {
    System.out.println("See all recent");
  }

  private void populateGrid(final FlowPane grid, final String[][] data) {
    grid.getChildren().clear();
    for (final String[] item : data) {
      // [0]=itemId, [1]=title, [2]=price, [3]=bids, [4]=timeLeft, [5]=badgeType,
      // [6]=category, [7]=section, [8]=description
      grid.getChildren().add(
          buildCard(item[0], item[1], item[2], item[3], item[4], item[5], item[6], item[8])
      );
    }
  }

  private VBox buildCard(final String itemId, final String title, final String price,
      final String bids, final String timeLeft, final String badgeType,
      final String category, final String description) {

    final StackPane imgBox = new StackPane();
    imgBox.setPrefWidth(150);
    imgBox.setPrefHeight(105);
    imgBox.getStyleClass().add("card-img-placeholder");
    imgBox.setStyle("-fx-background-color:#F4F4F4;-fx-background-radius:6px;");
    final Label noImg = new Label("No image");
    noImg.getStyleClass().add("caption");
    imgBox.getChildren().add(noImg);

    final Label catChip = new Label(category);
    catChip.getStyleClass().addAll("badge", "badge-info");

    final Label titleLabel = new Label(title);
    titleLabel.setWrapText(true);
    titleLabel.setMaxWidth(150);
    titleLabel.getStyleClass().add("body-small");
    titleLabel.setStyle("-fx-font-weight:bold;-fx-padding:4px 0;");

    final Label priceLabel = new Label(price);
    priceLabel.getStyleClass().add("price-tag");
    priceLabel.setStyle("-fx-font-size:15px;");
    // Register so refreshLivePrices() can update it when server responds
    priceLabels.put(itemId, priceLabel);

    final Label bidsLabel = new Label(bids);
    bidsLabel.getStyleClass().add("caption");

    final Label badge = new Label(timeLeft);
    badge.getStyleClass().addAll("badge",
        "success".equals(badgeType) ? "badge-success" : "badge-warning");

    final Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    final HBox metaRow = new HBox(bidsLabel, spacer, badge);
    metaRow.setAlignment(Pos.CENTER_LEFT);

    final VBox card = new VBox(4, imgBox, catChip, titleLabel, priceLabel, metaRow);
    card.setPrefWidth(160);
    card.getStyleClass().add("auction-card");

    card.setOnMouseClicked(e -> {
      UserSession.getInstance().setSelectedAuctionTitle(title);
      UserSession.getInstance().setSelectedAuctionCategory(category);
      UserSession.getInstance().setSelectedItemId(itemId);
      UserSession.getInstance().setSelectedItemDescription(description);
      SceneNavigator.navigateTo(SceneNavigator.View.AUCTION_DETAIL);
    });

    return card;
  }

  private void showEmpty(final boolean show) {
    emptyState.setVisible(show);
    emptyState.setManaged(show);
    endingSoonGrid.setVisible(!show);
    endingSoonGrid.setManaged(!show);
    recentGrid.setVisible(!show);
    recentGrid.setManaged(!show);
  }

  @FXML
  private void onToggleTheme() {
    SceneNavigator.toggleTheme();
  }

  @FXML
  private void onHome() {
    SceneNavigator.navigateTo(SceneNavigator.View.HOME);
  }
}