package com.auction.client.controllers;

import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.TopNavUtils;
import com.auction.client.utils.UserSession;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
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

  @FXML private StackPane rootPane;
  @FXML private TextField searchField;
  @FXML private Label userLabel;
  @FXML private Button walletBalanceBtn;
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

  private static final String[][] ALL_AUCTIONS = {
    {"Vintage Rolex Watch", "$1,240.00", "14 bids", "2h left",
        "warning", "Collectibles", "ending"},
    {"iPhone 15 Pro Max", "$780.00", "31 bids", "45m left",
        "warning", "Electronics", "ending"},
    {"Nike Air Jordan 1", "$210.00", "8 bids", "WINNING",
        "success", "Fashion", "ending"},
    {"Sony WH-1000XM5", "$190.00", "5 bids", "1h left",
        "warning", "Electronics", "ending"},
    {"Gaming Chair RGB", "$95.00", "2 bids", "3d left",
        "warning", "Home & Garden", "recent"},
    {"MacBook Air M2", "$850.00", "0 bids", "5d left",
        "warning", "Electronics", "recent"},
    {"Lego Star Wars Set", "$45.00", "1 bid", "4d left",
        "warning", "Collectibles", "recent"},
    {"Canon EOS R50", "$620.00", "3 bids", "2d left",
        "warning", "Electronics", "recent"},
    {"Mountain Bike 2024", "$320.00", "4 bids", "1d left",
        "warning", "Sports", "recent"},
    {"Toyota Camry 2018", "$8,500.00", "2 bids", "6d left",
        "warning", "Vehicles", "recent"},
    {"Vintage Denim Jacket", "$75.00", "6 bids", "2d left",
        "warning", "Fashion", "recent"},
    {"Garden Tool Set", "$55.00", "1 bid", "5d left",
        "warning", "Home & Garden", "recent"},
  };

  @Override
  public void initialize(final URL url, final ResourceBundle rb) {
    activeCategory = allCategoriesBtn;
    userLabel.setText(UserSession.getInstance().getInitials());
    TopNavUtils.updateWalletBalance(walletBalanceBtn);
    applyFilters();

    rootPane.widthProperty().addListener(
            (obs, oldW, newW) -> onWindowResized(newW.doubleValue()));
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
        .filter(item -> currentCategory == null || item[5].equals(currentCategory))
        .filter(item -> currentSearch.isEmpty() 
            || item[0].toLowerCase().contains(currentSearch))
        .toArray(String[][]::new);

    final String[][] ending = Arrays.stream(filtered)
        .filter(item -> "ending".equals(item[6]))
        .toArray(String[][]::new);

    final String[][] recent = Arrays.stream(filtered)
        .filter(item -> "recent".equals(item[6]))
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
      grid.getChildren().add(
          buildCard(item[0], item[1], item[2], item[3], item[4], item[5])
      );
    }
  }

  private VBox buildCard(final String title, final String price, final String bids,
      final String timeLeft, final String badgeType, final String category) {

    final StackPane imgBox = new StackPane();
    imgBox.setPrefWidth(150);
    imgBox.setPrefHeight(105);
    imgBox.setStyle("-fx-background-color:#F4F4F4;-fx-background-radius:6px;");
    final Label noImg = new Label("No image");
    noImg.setStyle("-fx-text-fill:#CCCCCC;-fx-font-size:11px;");
    imgBox.getChildren().add(noImg);

    final Label catChip = new Label(category);
    catChip.setStyle("-fx-font-size:10px;-fx-text-fill:#1A73E8;"
        + "-fx-background-color:#E8F0FE;"
        + "-fx-padding:1px 7px;-fx-background-radius:8px;");

    final Label titleLabel = new Label(title);
    titleLabel.setWrapText(true);
    titleLabel.setMaxWidth(150);
    titleLabel.setStyle("-fx-font-size:12px;-fx-font-weight:bold;"
        + "-fx-text-fill:#111111;-fx-padding:4px 0;");

    final Label priceLabel = new Label(price);
    priceLabel.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#E53238;");

    final Label bidsLabel = new Label(bids);
    bidsLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#888888;");

    final Label badge = new Label(timeLeft);
    final String baseStyle = "-fx-font-size:10px;-fx-font-weight:bold;"
        + "-fx-padding:2px 8px;-fx-background-radius:10px;";
    
    badge.setStyle(baseStyle + ("success".equals(badgeType)
        ? "-fx-background-color:#EAF5EA;-fx-text-fill:#5BA55B;"
        : "-fx-background-color:#FEF6E6;-fx-text-fill:#F5A623;"));

    final Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    final HBox metaRow = new HBox(bidsLabel, spacer, badge);
    metaRow.setAlignment(Pos.CENTER_LEFT);

    final VBox card = new VBox(4, imgBox, catChip, titleLabel, priceLabel, metaRow);
    card.setPrefWidth(160);

    final String normalStyle = "-fx-background-color:white;"
        + "-fx-border-color:#EBEBEB;-fx-border-width:1px;"
        + "-fx-border-radius:8px;-fx-background-radius:8px;"
        + "-fx-padding:10px;-fx-cursor:hand;";
    
    final String hoverStyle = "-fx-background-color:white;"
        + "-fx-border-color:#E53238;-fx-border-width:1.5px;"
        + "-fx-border-radius:8px;-fx-background-radius:8px;"
        + "-fx-padding:10px;-fx-cursor:hand;"
        + "-fx-scale-x:1.02;-fx-scale-y:1.02;";

    card.setStyle(normalStyle);
    card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
    card.setOnMouseExited(e -> card.setStyle(normalStyle));
    card.setOnMouseClicked(e -> {
      UserSession.getInstance().setSelectedAuctionTitle(title);
      UserSession.getInstance().setSelectedAuctionCategory(category);
      UserSession.getInstance().setSelectedAuctionPrice(price);
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

  @FXML
  private void onWallet() {
    SceneNavigator.navigateTo(SceneNavigator.View.WALLET);
  }
}