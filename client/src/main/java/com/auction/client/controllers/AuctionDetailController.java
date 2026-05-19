package com.auction.client.controllers;

import com.auction.client.services.BidService;
import com.auction.client.utils.InlineNotification;
import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.TopNavUtils;
import com.auction.client.utils.UserSession;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * AuctionDetailController handles the auction detail view.
 *
 * <p>Features: Countdown timer, bidding, realtime price curve, and dynamic history updates.
 */
public class AuctionDetailController implements Initializable {

  // UI Components
  @FXML private Label userLabel;
  @FXML private Button walletBalanceBtn;
  @FXML private Label backLabel;
  @FXML private Label itemTitle;
  @FXML private Label itemMeta;
  @FXML private Label itemDescription;
  @FXML private Label currentPrice;
  @FXML private Label totalBids;
  @FXML private Label totalBidders;
  @FXML private Label auctionStatus;
  @FXML private Label countdownTimer;
  @FXML private Label minBidHint;
  @FXML private TextField bidAmountField;
  @FXML private Label bidError;
  @FXML private Button placeBidBtn;
  @FXML private Button watchlistBtn;
  @FXML private VBox bidHistoryList;
  @FXML private Label noBidsLabel;
  @FXML private TextField maxPriceField;
  @FXML private TextField autoBidIncrementField;
  @FXML private Label autoBidError;
  @FXML private Button setupAutoBidBtn;
  @FXML private CheckBox aggressiveModeCheckBox;
  @FXML private VBox notificationBox;
  @FXML private Label notificationLabel;

  // Chart Components
  @FXML private LineChart<String, Number> bidHistoryChart;
  @FXML private CategoryAxis timeAxis;
  @FXML private NumberAxis priceAxis;
  private XYChart.Series<String, Number> priceSeries;

  // State Variables
  private static final int MAX_DATA_POINTS = 15;
  private final String currentAuctionId = "auction_123";
  private String currentUserId;
  private String currentHighestBidder = "";
  private int secondsRemaining = 6452;
  private Timeline countdownTimeline;
  private Timeline mockServerTimeline;
  private double currentMockPrice = 1240.00;

  // Initial dummy data
  private static final String[][] DUMMY_BIDS = {
    {"user_alpha", "$1,240.00", "2 min ago", "winning"},
    {"buyer_99", "$1,180.00", "8 min ago", ""},
    {"collector_vn", "$1,050.00", "22 min ago", ""}
  };

  @Override
  public void initialize(final URL url, final ResourceBundle rb) {
    currentUserId = UserSession.getInstance().getUsername();
    userLabel.setText(UserSession.getInstance().getInitials());
    TopNavUtils.updateWalletBalance(walletBalanceBtn);

    setupItemDetails();
    setupChart();
    
    totalBids.setText("14");
    totalBidders.setText("7");
    auctionStatus.setText("OPEN");

    startCountdown();
    loadBidHistory();
    currentHighestBidder = DUMMY_BIDS.length > 0 ? DUMMY_BIDS[0][0] : "";

    setupServiceCallbacks();
    startMockRealtimeBids(); // Khởi chạy hệ thống giả lập Server (Mocking)
  }

  private void setupItemDetails() {
    final String clickedTitle = UserSession.getInstance().getSelectedAuctionTitle();
    final String clickedCategory = UserSession.getInstance().getSelectedAuctionCategory();

    itemTitle.setText(clickedTitle != null ? clickedTitle : "Vintage Rolex Submariner 1969");
    itemMeta.setText((clickedCategory != null ? clickedCategory : "Accessories") 
        + "  ·  Seller: auto_seller");
    itemDescription.setText("Original 1969 Rolex Submariner in excellent condition. "
        + "Original bracelet, box, and papers included. Serviced in 2022. "
        + "Running perfectly with minor surface scratches.");
    
    final String clickedPrice = UserSession.getInstance().getSelectedAuctionPrice();
    if (clickedPrice != null && !clickedPrice.isEmpty()) {
      try {
        currentMockPrice = Double.parseDouble(clickedPrice.replace("$", "").replace(",", ""));
      } catch (NumberFormatException ex) {
        currentMockPrice = 1240.0;
      }
    }
    updatePrice(currentMockPrice);
  }

  private void setupChart() {
    priceSeries = new XYChart.Series<>();
    priceSeries.setName("Highest Bid Trend");
    bidHistoryChart.getData().add(priceSeries);
    bidHistoryChart.setCreateSymbols(true); // Hiển thị các chấm tròn trên đồ thị
  }

  private void setupServiceCallbacks() {
    BidService.getInstance().requestStatus();

    BidService.getInstance().setCallbacks(
        this::updatePrice,
        transaction -> {
          final String priceStr = String.format("$%.2f", transaction.getBidAmount());
          final boolean isWinning = transaction.getBidderId().equals(currentUserId);
          
          if (isWinning) {
            InlineNotification.show(notificationBox, notificationLabel,
                "Your bid was placed successfully.", true);
          } else if (currentHighestBidder.equals(currentUserId)) {
            InlineNotification.show(notificationBox, notificationLabel,
                "You were outbid by " + transaction.getBidderId() + ".", false);
          }
          
          currentHighestBidder = transaction.getBidderId();
          final String badge = isWinning ? "winning" : "";
          addBidRowToHistory(transaction.getBidderId(), priceStr, "just now", badge);
          updateChartData(transaction.getBidAmount());
          
          noBidsLabel.setVisible(false);
          noBidsLabel.setManaged(false);
          totalBids.setText(String.valueOf(Integer.parseInt(totalBids.getText()) + 1));
        });

    bidAmountField.textProperty().addListener((obs, old, val) -> bidError.setText(""));
    autoBidIncrementField.setText(String.valueOf(BidService.getInstance().getMinimumIncrement()));
    maxPriceField.textProperty().addListener((obs, old, val) -> autoBidError.setText(""));
    autoBidIncrementField.textProperty().addListener((o, old, val) -> autoBidError.setText(""));

    BidService.getInstance().setOnPriceChangeNotification(msg -> 
        InlineNotification.show(notificationBox, notificationLabel, msg, true));

    BidService.getInstance().setOnBidError(msg -> {
      bidError.setText(msg);
      InlineNotification.show(notificationBox, notificationLabel, msg, false);
    });
  }

  /**
   * Giả lập việc Server gửi giá thầu mới liên tục (Mocking).
   * Trong tương lai, em sẽ thay thế logic này bằng Socket.
   */
  private void startMockRealtimeBids() {
    final Random random = new Random();
    final String[] mockUsers = {"john_doe", "sniper88", "crypto_king", "watch_lover"};

    mockServerTimeline = new Timeline(new KeyFrame(Duration.seconds(8), e -> {
      if (secondsRemaining <= 0) {
        mockServerTimeline.stop();
        return;
      }
      // Giả lập một người khác đặt giá ngẫu nhiên cao hơn 10$ - 100$
      final double increment = 10.0 + (random.nextDouble() * 90.0);
      currentMockPrice += increment;
      final String randomUser = mockUsers[random.nextInt(mockUsers.length)];
      
      // Cập nhật UI an toàn trên luồng JavaFX
      Platform.runLater(() -> {
        currentHighestBidder = randomUser;
        final String priceStr = String.format("$%.2f", currentMockPrice);
        
        updatePrice(currentMockPrice);
        addBidRowToHistory(randomUser, priceStr, "just now", "new");
        updateChartData(currentMockPrice);
        
        totalBids.setText(String.valueOf(Integer.parseInt(totalBids.getText()) + 1));
      });
    }));
    
    mockServerTimeline.setCycleCount(Timeline.INDEFINITE);
    mockServerTimeline.play();
  }

  private void updateChartData(final double newPrice) {
    final String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    final XYChart.Data<String, Number> newDataPoint = new XYChart.Data<>(currentTime, newPrice);
    
    priceSeries.getData().add(newDataPoint);
    if (priceSeries.getData().size() > MAX_DATA_POINTS) {
      priceSeries.getData().remove(0);
    }
  }

  private void updatePrice(final double amount) {
    currentMockPrice = amount;
    currentPrice.setText(String.format("$%.2f", amount));
    final double nextMin = amount + BidService.getInstance().getMinimumIncrement();
    minBidHint.setText(String.format("Minimum bid: $%.2f", nextMin));
  }

  private void startCountdown() {
    countdownTimeline = new Timeline(
        new KeyFrame(Duration.seconds(1), e -> {
          if (secondsRemaining > 0) {
            secondsRemaining--;
            updateTimerDisplay();
          } else {
            countdownTimer.setText("ENDED");
            countdownTimeline.stop();
            onAuctionEnded();
          }
        }));
    countdownTimeline.setCycleCount(Timeline.INDEFINITE);
    countdownTimeline.play();
    updateTimerDisplay();
  }

  private void updateTimerDisplay() {
    final int h = secondsRemaining / 3600;
    final int m = (secondsRemaining % 3600) / 60;
    final int s = secondsRemaining % 60;
    countdownTimer.setText(String.format("%02d:%02d:%02d", h, m, s));

    if (secondsRemaining < 60) {
      countdownTimer.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#E53238;");
    } else {
      countdownTimer.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#F5A623;");
    }
  }

  private void onAuctionEnded() {
    placeBidBtn.setDisable(true);
    bidAmountField.setDisable(true);
    auctionStatus.setText("FINISHED");
    auctionStatus.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#E53238;");
    BidService.getInstance().setAuctionClosed();
    setupAutoBidBtn.setDisable(true);
    maxPriceField.setDisable(true);
    autoBidIncrementField.setDisable(true);
    if (mockServerTimeline != null) {
      mockServerTimeline.stop();
    }
  }

  @FXML
  private void onPlaceBid() {
    final String input = bidAmountField.getText().trim();
    if (input.isEmpty()) {
      bidError.setText("Please enter a bid amount");
      return;
    }
    double amount;
    try {
      amount = Double.parseDouble(input.replace(",", ""));
    } catch (NumberFormatException ex) {
      bidError.setText("Please enter a valid number");
      return;
    }

    final String errorMsg = BidService.getInstance()
        .placeBid(currentUserId, currentAuctionId, amount);

    if (errorMsg != null) {
      bidError.setText(errorMsg);
    } else {
      bidAmountField.clear();
      bidError.setText("");
    }
  }

  @FXML
  private void onSetupAutoBid() {
    // Logic auto bid giữ nguyên...
  }

  @FXML
  private void onAddWatchlist() {
    watchlistBtn.setText("Added to watchlist");
    watchlistBtn.setDisable(true);
    InlineNotification.show(notificationBox, notificationLabel, "Added to watchlist.", true);
  }

  @FXML private void onBack() { 
    SceneNavigator.navigateTo(SceneNavigator.View.HOME); 
  }

  @FXML private void onHome() { 
    SceneNavigator.navigateTo(SceneNavigator.View.HOME); 
  }

  @FXML private void onMyBids() { 
    SceneNavigator.navigateTo(SceneNavigator.View.MY_BIDS); 
  }

  @FXML private void onWatchlist() { 
    SceneNavigator.navigateTo(SceneNavigator.View.MY_BIDS); 
  } 

  @FXML private void onSell() { 
    SceneNavigator.navigateTo(SceneNavigator.View.CREATE_LISTING); 
  }

  @FXML private void onWallet() { 
    SceneNavigator.navigateTo(SceneNavigator.View.WALLET); 
  }

  @FXML private void onProfile() { 
    SceneNavigator.navigateTo(SceneNavigator.View.PROFILE); 
  }

  @FXML private void onToggleTheme() { 
    SceneNavigator.toggleTheme(); 
  }

  private void loadBidHistory() {
    if (DUMMY_BIDS.length == 0) {
      noBidsLabel.setVisible(true);
      return;
    }
    noBidsLabel.setVisible(false);
    noBidsLabel.setManaged(false);
    
    // Đẩy dữ liệu cũ vào đồ thị trước
    for (int i = DUMMY_BIDS.length - 1; i >= 0; i--) {
      double oldPrice = Double.parseDouble(DUMMY_BIDS[i][1].replace("$", "").replace(",", ""));
      updateChartData(oldPrice);
    }

    for (String[] bid : DUMMY_BIDS) {
      addBidRowToHistory(bid[0], bid[1], bid[2], bid[3]);
    }
  }

  /**
   * Tạo dòng UI hiển thị lịch sử đặt giá, được làm nổi bật như một chiếc thẻ bo góc.
   */
  private void addBidRowToHistory(final String name, final String price,
      final String time, final String badge) {
    
    final HBox row = new HBox();
    row.setSpacing(12);
    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    
    // Tạo hiệu ứng thẻ nổi bật cho các bid mới nhất
    if ("winning".equals(badge) || "new".equals(badge)) {
      row.setStyle("-fx-background-color:#F5F9FF;-fx-border-color:#D0E2FF;"
          + "-fx-border-radius:6px;-fx-background-radius:6px;-fx-padding:10px;");
    } else {
      row.setStyle("-fx-background-color:transparent;-fx-border-color:#F4F4F4;"
          + "-fx-border-width:0 0 1px 0;-fx-padding:10px;");
    }

    // Câu highlight lịch sử: "Người 1 đặt 1200$"
    final Label actionLbl = new Label(name + " đã đặt");
    actionLbl.setStyle("-fx-font-size:13px;-fx-text-fill:#555;-fx-font-family:'Segoe UI';");
    HBox.setHgrow(actionLbl, Priority.ALWAYS);

    final Label priceLbl = new Label(price);
    priceLbl.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#E53238;");

    final Label timeLbl = new Label(time);
    timeLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#AAAAAA;"
        + "-fx-min-width:60px;-fx-alignment:CENTER_RIGHT;");
    row.getChildren().addAll(actionLbl, priceLbl, timeLbl);

    // Xóa dòng cuối nếu danh sách quá dài để giao diện không bị giật
    if (bidHistoryList.getChildren().size() >= 20) {
      bidHistoryList.getChildren().remove(bidHistoryList.getChildren().size() - 1);
    }
    
    bidHistoryList.getChildren().add(0, row);
  }
}