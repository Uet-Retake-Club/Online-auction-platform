package com.auction.client.controllers;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

import com.auction.client.utils.SceneNavigator;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
 * HomeController.java
 *
 * THAY ĐỔI SO VỚI PHIÊN BẢN CŨ:
 *
 * 1. RESPONSIVE / SCALE UP KHI FULLSCREEN
 *    - Lắng nghe widthProperty() của rootPane
 *    - Khi cửa sổ rộng hơn → card rộng hơn, FlowPane tự wrap lại
 *    - Card width = (availableWidth - sidebar - padding) / số cột tối ưu
 *
 * 2. CATEGORY FILTER THẬT SỰ
 *    - Mỗi item trong dummy data có thêm field "category"
 *    - filterByCategory(category) lọc đúng dữ liệu theo tên category
 *    - "All categories" → hiển thị tất cả
 *    - Các category khác → chỉ hiển thị items khớp
 *    - Kết hợp được với search: filter = category + search cùng lúc
 *
 * 3. SEARCH + FILTER KẾT HỢP
 *    - applyFilters() được gọi bởi cả search lẫn category click
 *    - Không cần reload lại tất cả mỗi lần
 *
 * NOTE CHO BACKEND:
 *    Khi có AuctionService, thay ALL_AUCTIONS bằng:
 *    List<Auction> auctions = AuctionService.getAll();
 *    Rồi filter bằng: auctions.stream()
 *        .filter(a -> category == null || a.getCategory().equals(category))
 *        .filter(a -> query == null || a.getTitle().toLowerCase().contains(query))
 *        .collect(...)
 */
public class HomeController implements Initializable {

    // ── FXML nodes ───────────────────────────────────────────
    @FXML private BorderPane rootPane;
    @FXML private TextField  searchField;
    @FXML private Label      userLabel;

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
    @FXML private VBox     emptyState;
    @FXML private VBox     loadingState;
    @FXML private Label    seeAllEndingSoon;
    @FXML private Label    seeAllRecent;

    // ── State ─────────────────────────────────────────────────
    private Button activeCategory;
    private String currentCategory = null; // null = all
    private String currentSearch   = "";

    // ── Dummy data — Format: { title, price, bids, timeLeft, badgeType, category, section }
    // section: "ending" hoặc "recent"
    // category: phải khớp CHÍNH XÁC với text của sidebar button
    private static final String[][] ALL_AUCTIONS = {
        // Ending soon
        { "Vintage Rolex Watch",   "$1,240.00", "14 bids", "2h left",  "warning", "Collectibles", "ending" },
        { "iPhone 15 Pro Max",     "$780.00",   "31 bids", "45m left", "warning", "Electronics",  "ending" },
        { "Nike Air Jordan 1",     "$210.00",   "8 bids",  "WINNING",  "success", "Fashion",      "ending" },
        { "Sony WH-1000XM5",       "$190.00",   "5 bids",  "1h left",  "warning", "Electronics",  "ending" },
        // Recently listed
        { "Gaming Chair RGB",      "$95.00",    "2 bids",  "3d left",  "warning", "Home & Garden","recent" },
        { "MacBook Air M2",        "$850.00",   "0 bids",  "5d left",  "warning", "Electronics",  "recent" },
        { "Lego Star Wars Set",    "$45.00",    "1 bid",   "4d left",  "warning", "Collectibles", "recent" },
        { "Canon EOS R50",         "$620.00",   "3 bids",  "2d left",  "warning", "Electronics",  "recent" },
        { "Mountain Bike 2024",    "$320.00",   "4 bids",  "1d left",  "warning", "Sports",       "recent" },
        { "Toyota Camry 2018",     "$8,500.00", "2 bids",  "6d left",  "warning", "Vehicles",     "recent" },
        { "Vintage Denim Jacket",  "$75.00",    "6 bids",  "2d left",  "warning", "Fashion",      "recent" },
        { "Garden Tool Set",       "$55.00",    "1 bid",   "5d left",  "warning", "Home & Garden","recent" },
    };

    // ── Lifecycle ─────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        activeCategory = allCategoriesBtn;
        userLabel.setText("JD"); // TODO: UserSession.getInstance().getInitials()

        // Hiển thị tất cả khi mới vào
        applyFilters();

        // ── RESPONSIVE: lắng nghe width của cửa sổ ──────────
        // Mỗi khi cửa sổ thay đổi kích thước → tính lại card width
        rootPane.widthProperty().addListener(
            (obs, oldW, newW) -> onWindowResized(newW.doubleValue())
        );
    }

    // ── RESPONSIVE LOGIC ─────────────────────────────────────

    /**
     * Tính lại kích thước card khi cửa sổ thay đổi.
     *
     * Công thức:
     *   availableWidth = windowWidth - sidebarWidth(190) - scrollbarWidth(10) - padding(44)
     *   numColumns     = max(2, availableWidth / 200)   ← tối thiểu 2 cột
     *   cardWidth      = (availableWidth / numColumns) - gap(14)
     */
    private void onWindowResized(double windowWidth) {
        double available = windowWidth - 190 - 10 - 44;
        int    numCols   = Math.max(2, (int)(available / 200));
        double cardWidth = (available / numCols) - 14;
        cardWidth = Math.max(150, Math.min(cardWidth, 280)); // clamp 150–280px

        // Cập nhật width cho tất cả card đang hiển thị
        updateCardWidths(endingSoonGrid, cardWidth);
        updateCardWidths(recentGrid,     cardWidth);
    }

    private void updateCardWidths(FlowPane grid, double cardWidth) {
        grid.getChildren().forEach(node -> {
            if (node instanceof VBox card) {
                card.setPrefWidth(cardWidth);
                // Image box cũng scale theo
                if (!card.getChildren().isEmpty()
                    && card.getChildren().get(0) instanceof StackPane imgBox) {
                    imgBox.setPrefWidth(cardWidth - 20);
                    // Tỉ lệ ảnh 3:2
                    imgBox.setPrefHeight((cardWidth - 20) * 0.65);
                }
                // Title label wrap theo card width
                card.getChildren().stream()
                    .filter(n -> n instanceof Label)
                    .map(n -> (Label) n)
                    .findFirst()
                    .ifPresent(lbl -> lbl.setMaxWidth(cardWidth - 20));
            }
        });
    }

    // ── SEARCH ────────────────────────────────────────────────

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

    // ── CATEGORY FILTER ───────────────────────────────────────

    @FXML private void onCategoryAll()          { switchCategory(allCategoriesBtn, null);            }
    @FXML private void onCategoryElectronics()  { switchCategory(electronicsBtn,   "Electronics");   }
    @FXML private void onCategoryFashion()      { switchCategory(fashionBtn,       "Fashion");       }
    @FXML private void onCategoryHome()         { switchCategory(homeGardenBtn,    "Home & Garden"); }
    @FXML private void onCategorySports()       { switchCategory(sportsBtn,        "Sports");        }
    @FXML private void onCategoryCollectibles() { switchCategory(collectiblesBtn,  "Collectibles");  }
    @FXML private void onCategoryVehicles()     { switchCategory(vehiclesBtn,      "Vehicles");      }
    @FXML private void onCategoryOther()        { switchCategory(otherBtn,         "Other");         }

    private void switchCategory(Button btn, String category) {
        // Cập nhật highlight sidebar
        if (activeCategory != null) {
            activeCategory.getStyleClass().remove("nav-item-active");
            activeCategory.getStyleClass().add("nav-item");
        }
        btn.getStyleClass().remove("nav-item");
        btn.getStyleClass().add("nav-item-active");
        activeCategory    = btn;
        currentCategory   = category;

        applyFilters();
    }

    // ── CORE FILTER LOGIC ─────────────────────────────────────

    /**
     * Lọc ALL_AUCTIONS theo currentCategory + currentSearch,
     * rồi phân chia vào 2 grid: endingSoon và recentGrid.
     *
     * Đây là nơi cắm AuctionService sau này:
     *   List<Auction> filtered = AuctionService.getAll()
     *       .stream()
     *       .filter(a -> currentCategory == null
     *                    || a.getCategory().equals(currentCategory))
     *       .filter(a -> currentSearch.isEmpty()
     *                    || a.getTitle().toLowerCase().contains(currentSearch))
     *       .collect(Collectors.toList());
     */
    private void applyFilters() {
        // Lọc từ dummy data
        String[][] filtered = Arrays.stream(ALL_AUCTIONS)
            .filter(item -> {
                // Filter theo category (index 5)
                boolean catOk = (currentCategory == null)
                    || item[5].equals(currentCategory);
                // Filter theo search text (index 0 = title)
                boolean searchOk = currentSearch.isEmpty()
                    || item[0].toLowerCase().contains(currentSearch);
                return catOk && searchOk;
            })
            .toArray(String[][]::new);

        // Phân chia theo section (index 6)
        String[][] endingSoon = Arrays.stream(filtered)
            .filter(item -> "ending".equals(item[6]))
            .toArray(String[][]::new);

        String[][] recent = Arrays.stream(filtered)
            .filter(item -> "recent".equals(item[6]))
            .toArray(String[][]::new);

        // Hiển thị
        if (filtered.length == 0) {
            showEmpty(true);
        } else {
            showEmpty(false);
            populateGrid(endingSoonGrid, endingSoon);
            populateGrid(recentGrid,     recent);

            // Áp dụng responsive width ngay sau khi populate
            if (rootPane.getWidth() > 0) {
                onWindowResized(rootPane.getWidth());
            }
        }
    }

    // ── Navigation ────────────────────────────────────────────

    @FXML private void onSell()      { SceneNavigator.navigateTo(SceneNavigator.View.CREATE_LISTING); }
    @FXML private void onWatchlist() { SceneNavigator.navigateTo(SceneNavigator.View.MY_BIDS); }
    @FXML private void onMyBids()    { SceneNavigator.navigateTo(SceneNavigator.View.MY_BIDS); }
    @FXML private void onProfile()   { SceneNavigator.navigateTo(SceneNavigator.View.PROFILE); }
    @FXML private void onLogout()    {
        // TODO: UserSession.getInstance().clear();
        SceneNavigator.navigateTo(SceneNavigator.View.LOGIN);
    }
    @FXML private void onSeeAllEndingSoon() { System.out.println("See all ending soon"); }
    @FXML private void onSeeAllRecent()     { System.out.println("See all recent"); }

    // ── Card builder ──────────────────────────────────────────

    private void populateGrid(FlowPane grid, String[][] data) {
        grid.getChildren().clear();
        for (String[] item : data) {
            grid.getChildren().add(
                buildCard(item[0], item[1], item[2], item[3], item[4], item[5])
            );
        }
    }

    private VBox buildCard(String title, String price, String bids,
                           String timeLeft, String badgeType, String category) {

        // ── Image placeholder ─────────────────────────────────
        StackPane imgBox = new StackPane();
        imgBox.setPrefWidth(150);
        imgBox.setPrefHeight(105);
        imgBox.setStyle("-fx-background-color: #F4F4F4; -fx-background-radius: 6px;");

        Label noImg = new Label("No image");
        noImg.setStyle("-fx-text-fill: #CCCCCC; -fx-font-size: 11px; -fx-font-family: 'Segoe UI';");
        imgBox.getChildren().add(noImg);

        // ── Category chip ─────────────────────────────────────
        Label catChip = new Label(category);
        catChip.setStyle("-fx-font-size: 10px; -fx-text-fill: #1A73E8;"
                       + "-fx-background-color: #E8F0FE;"
                       + "-fx-padding: 1px 7px; -fx-background-radius: 8px;"
                       + "-fx-font-family: 'Segoe UI';");

        // ── Title ─────────────────────────────────────────────
        Label titleLabel = new Label(title);
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(150);
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;"
                          + "-fx-text-fill: #111111; -fx-font-family: 'Segoe UI';"
                          + "-fx-padding: 4px 0 4px 0;");

        // ── Price ─────────────────────────────────────────────
        Label priceLabel = new Label(price);
        priceLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;"
                          + "-fx-text-fill: #E53238; -fx-font-family: 'Segoe UI';");

        // ── Bids + badge row ──────────────────────────────────
        Label bidsLabel = new Label(bids);
        bidsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888; -fx-font-family: 'Segoe UI';");

        Label badge = new Label(timeLeft);
        String baseStyle = "-fx-font-size: 10px; -fx-font-weight: bold;"
                         + "-fx-padding: 2px 8px; -fx-background-radius: 10px;";
        badge.setStyle(baseStyle + ("success".equals(badgeType)
            ? "-fx-background-color: #EAF5EA; -fx-text-fill: #5BA55B;"
            : "-fx-background-color: #FEF6E6; -fx-text-fill: #F5A623;"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox metaRow = new HBox(bidsLabel, spacer, badge);
        metaRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // ── Assemble card ─────────────────────────────────────
        VBox card = new VBox(4, imgBox, catChip, titleLabel, priceLabel, metaRow);
        card.setPrefWidth(160);
        card.setStyle("-fx-background-color: white;"
                    + "-fx-border-color: #EBEBEB; -fx-border-width: 1px;"
                    + "-fx-border-radius: 8px; -fx-background-radius: 8px;"
                    + "-fx-padding: 10px; -fx-cursor: hand;");

        // ── Hover effect ──────────────────────────────────────
        String normalStyle = "-fx-background-color: white;"
            + "-fx-border-color: #EBEBEB; -fx-border-width: 1px;"
            + "-fx-border-radius: 8px; -fx-background-radius: 8px;"
            + "-fx-padding: 10px; -fx-cursor: hand;";
        String hoverStyle  = "-fx-background-color: white;"
            + "-fx-border-color: #E53238; -fx-border-width: 1.5px;"
            + "-fx-border-radius: 8px; -fx-background-radius: 8px;"
            + "-fx-padding: 10px; -fx-cursor: hand;"
            + "-fx-scale-x: 1.02; -fx-scale-y: 1.02;";

        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e  -> card.setStyle(normalStyle));
        card.setOnMouseClicked(e ->
            SceneNavigator.navigateTo(SceneNavigator.View.AUCTION_DETAIL)
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
