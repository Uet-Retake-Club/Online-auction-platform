package com.auction.client.controllers;

import com.auction.client.services.BidService;
import com.auction.client.utils.ConfirmBidDialog;
import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.ToastNotification;
import com.auction.client.utils.UserSession;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
 * <p>Features: Countdown timer, bidding, and dynamic history updates.
 */
public class AuctionDetailController implements Initializable {

  @FXML private Label userLabel;
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

  private final String currentAuctionId = "auction_123";
  private String currentUserId;
  private String currentHighestBidder = "";
  private int secondsRemaining = 6452;
  private Timeline countdownTimeline;

  private static final String[][] DUMMY_BIDS = {
    {"user_alpha", "$1,240.00", "2 min ago", "winning"},
    {"buyer_99", "$1,180.00", "8 min ago", ""},
    {"collector_vn", "$1,050.00", "22 min ago", ""}
  };

  @Override
  public void initialize(final URL url, final ResourceBundle rb) {
    currentUserId = UserSession.getInstance().getUsername();
    userLabel.setText(UserSession.getInstance().getInitials());

    itemTitle.setText("Vintage Rolex Submariner 1969");
    itemMeta.setText("Collectibles  ·  Seller: watch_king99");
    itemDescription.setText("Original 1969 Rolex Submariner in excellent condition. "
        + "Original bracelet, box, and papers included. Serviced in 2022. "
        + "Running perfectly with minor surface scratches.");
    
    updatePrice(BidService.getInstance().getCurrentBidAmount());
    BidService.getInstance().requestStatus();
    totalBids.setText("14");
    totalBidders.setText("7");
    auctionStatus.setText("OPEN");

    startCountdown();
    loadBidHistory();
    currentHighestBidder = DUMMY_BIDS.length > 0 ? DUMMY_BIDS[0][0] : "";

    BidService.getInstance().setCallbacks(
        this::updatePrice,
        transaction -> {
          final String priceStr = String.format("$%.2f", transaction.getBidAmount());
          final boolean isWinning = transaction.getBidderId().equals(currentUserId);
          
          if (isWinning) {
            ToastNotification.show(userLabel, "Your bid was placed successfully.", 
                ToastNotification.Type.SUCCESS);
          } else if (currentHighestBidder.equals(currentUserId)) {
            ToastNotification.show(userLabel, 
                "You were outbid by " + transaction.getBidderId() + ".",
                ToastNotification.Type.WARNING);
          }
          currentHighestBidder = transaction.getBidderId();
          final String badge = isWinning ? "winning" : "";
          addBidRowToHistory(transaction.getBidderId(), priceStr, "just now", badge);
          noBidsLabel.setVisible(false);
          noBidsLabel.setManaged(false);
          final int currentTotal = Integer.parseInt(totalBids.getText());
          totalBids.setText(String.valueOf(currentTotal + 1));
        });

    bidAmountField.textProperty().addListener((obs, old, val) -> bidError.setText(""));
    autoBidIncrementField.setText(
        String.valueOf(BidService.getInstance().getMinimumIncrement()));
    maxPriceField.textProperty().addListener((obs, old, val) -> autoBidError.setText(""));
    autoBidIncrementField.textProperty().addListener((o, old, val) -> autoBidError.setText(""));

    BidService.getInstance().setOnPriceChangeNotification(msg -> 
        ToastNotification.show(userLabel, msg, ToastNotification.Type.INFO));

    BidService.getInstance().setOnAutoBidResult(response -> {
      if ("SUCCESS".equals(response.getStatus())) {
        setupAutoBidBtn.setText("Auto-Bid Active ✓");
        setupAutoBidBtn.setStyle("-fx-background-color:#5BA55B;-fx-text-fill:white;"
            + "-fx-font-weight:bold;-fx-font-size:12px;-fx-padding:8px;");
        ToastNotification.show(userLabel, "Auto-Bid activated!", ToastNotification.Type.SUCCESS);
      } else {
        autoBidError.setText(response.getMessage());
        ToastNotification.show(userLabel, response.getMessage(), ToastNotification.Type.DANGER);
      }
    });

    BidService.getInstance().setOnBidError(msg -> {
      bidError.setText(msg);
      ToastNotification.show(userLabel, msg, ToastNotification.Type.DANGER);
    });
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

    if (!ConfirmBidDialog.show("Confirm bid", String.format("Place bid of $%.2f?", amount))) {
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
    final String maxStr = maxPriceField.getText().trim();
    final String incStr = autoBidIncrementField.getText().trim();

    if (maxStr.isEmpty() || incStr.isEmpty()) {
      autoBidError.setText("Fill both fields");
      return;
    }

    try {
      final double maxPrice = Double.parseDouble(maxStr.replace(",", ""));
      final double increment = Double.parseDouble(incStr.replace(",", ""));
      final boolean isAggressive = aggressiveModeCheckBox.isSelected();

      final String errorMsg = BidService.getInstance()
          .setupAutoBid(currentUserId, currentAuctionId, maxPrice, increment, isAggressive);

      if (errorMsg != null) {
        autoBidError.setText(errorMsg);
      } else {
        setupAutoBidBtn.setText("Sending...");
        autoBidError.setText("");
      }
    } catch (NumberFormatException ex) {
      autoBidError.setText("Invalid numbers");
    }
  }

  @FXML
  private void onAddWatchlist() {
    watchlistBtn.setText("Added to watchlist");
    watchlistBtn.setDisable(true);
    ToastNotification.show(userLabel, "Added to watchlist.", ToastNotification.Type.INFO);
  }

  @FXML
  private void onBack() {
    SceneNavigator.navigateTo(SceneNavigator.View.HOME);
  }

  @FXML
  private void onHome() {
    SceneNavigator.navigateTo(SceneNavigator.View.HOME);
  }

  @FXML
  private void onMyBids() {
    SceneNavigator.navigateTo(SceneNavigator.View.MY_BIDS);
  }

  @FXML
  private void onWatchlist() {
    SceneNavigator.navigateTo(SceneNavigator.View.MY_BIDS);
  }

  @FXML
  private void onSell() {
    SceneNavigator.navigateTo(SceneNavigator.View.CREATE_LISTING);
  }

  @FXML
  private void onProfile() {
    SceneNavigator.navigateTo(SceneNavigator.View.PROFILE);
  }

  private void updatePrice(final double amount) {
    currentPrice.setText(String.format("$%.2f", amount));
    final double nextMin = amount + BidService.getInstance().getMinimumIncrement();
    minBidHint.setText(String.format("Minimum bid: $%.2f", nextMin));
  }

  private void loadBidHistory() {
    if (DUMMY_BIDS.length == 0) {
      noBidsLabel.setVisible(true);
      return;
    }
    noBidsLabel.setVisible(false);
    noBidsLabel.setManaged(false);
    for (String[] bid : DUMMY_BIDS) {
      addBidRowToHistory(bid[0], bid[1], bid[2], bid[3]);
    }
  }

  private void addBidRowToHistory(final String name, final String price,
      final String time, final String badge) {
    final HBox row = new HBox();
    row.setStyle("-fx-border-color:transparent transparent #F4F4F4 transparent;"
        + "-fx-border-width:0 0 1px 0;-fx-padding:8px 0;-fx-alignment:CENTER_LEFT;");
    row.setSpacing(8);

    final Label nameLbl = new Label(name);
    nameLbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#333;");
    HBox.setHgrow(nameLbl, Priority.ALWAYS);

    final Label priceLbl = new Label(price);
    priceLbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#E53238;");

    final Label timeLbl = new Label(time);
    timeLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#AAAAAA;-fx-min-width:80px;");

    row.getChildren().addAll(nameLbl, priceLbl, timeLbl);

    if (!badge.isEmpty()) {
      final Label bdg = new Label("Winning");
      bdg.setStyle("-fx-background-color:#EAF5EA;-fx-text-fill:#5BA55B;"
          + "-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:2px 8px;");
      row.getChildren().add(bdg);
    }
    bidHistoryList.getChildren().add(0, row);
  }
}