package com.auction.client.controllers;

import com.auction.client.services.NetworkClientService;
import com.auction.client.services.BidService;
import com.auction.shared.dto.Request;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * AuctionDetailController handles the auction detail view.
 *
 * <p>Features: Countdown timer, bidding, and dynamic history updates.
 */
import javafx.scene.layout.BorderPane;
public class AuctionDetailController implements Initializable {

  @FXML private BorderPane rootPane;
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
  @FXML private VBox imageContainer;
  @FXML private TextField maxPriceField;
  @FXML private TextField autoBidIncrementField;
  @FXML private Label autoBidError;
  @FXML private Button setupAutoBidBtn;
  @FXML private CheckBox aggressiveModeCheckBox;
  

  private String currentAuctionId;
  private String currentUserId;
  private String currentHighestBidder = "";
  private int secondsRemaining = 6452;
  private Timeline countdownTimeline;

  @Override
  public void initialize(final URL url, final ResourceBundle rb) {
    currentUserId = UserSession.getInstance().getUserId();
    // Read item info set by HomeController on card click
    currentAuctionId = UserSession.getInstance().getSelectedItemId();
    

    // ── 0. Reset stale state from any previously viewed item ──
    BidService.getInstance().resetForItem();

    final String clickedTitle = UserSession.getInstance().getSelectedAuctionTitle();
    final String clickedCategory = UserSession.getInstance().getSelectedAuctionCategory();
    final String clickedDesc = UserSession.getInstance().getSelectedItemDescription();

    itemTitle.setText(clickedTitle != null && !clickedTitle.isEmpty() ? clickedTitle : "Auction Item");
    itemMeta.setText((clickedCategory != null && !clickedCategory.isEmpty() ? clickedCategory : "General")
        + "  ·  Seller: admin");
    itemDescription.setText(clickedDesc != null && !clickedDesc.isEmpty() ? clickedDesc
        : "No description available.");

    final byte[] imgData = UserSession.getInstance().getSelectedItemImageData();
    if (imgData != null && imgData.length > 0) {
      final VBox placeholder = new VBox(new Label("Loading image..."));
      placeholder.setAlignment(javafx.geometry.Pos.CENTER);
      placeholder.setStyle("-fx-background-color: -bg-surface-alt; -fx-background-radius: 12px;");
      placeholder.prefWidthProperty().bind(imageContainer.widthProperty().subtract(40));
      placeholder.prefHeightProperty().bind(imageContainer.heightProperty().subtract(40));
      imageContainer.getChildren().clear();
      imageContainer.getChildren().add(placeholder);

      java.util.concurrent.CompletableFuture.supplyAsync(() -> {
        try {
          return new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imgData));
        } catch (Exception e) {
          return null;
        }
      }).thenAcceptAsync(img -> {
        imageContainer.getChildren().clear();
        if (img != null && !img.isError()) {
          javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView(img);
          imgView.setPreserveRatio(true);
          imgView.fitWidthProperty().bind(imageContainer.widthProperty().subtract(40));
          imgView.fitHeightProperty().bind(imageContainer.heightProperty().subtract(40));
          imageContainer.getChildren().add(imgView);
        } else {
          imageContainer.getChildren().add(new Label("Failed to load image"));
        }
      }, javafx.application.Platform::runLater);
    }

    // ── 1. Wire ALL callbacks FIRST — before sending any network requests ──
    BidService.getInstance().setCallbacks(
        this::updatePrice,
        transaction -> {
          rebuildBidHistoryUI();

          final String priceStr = String.format("$%.2f", transaction.getBidAmount());
          final boolean isMyBid = transaction.getBidderId().equals(currentUserId)
              || transaction.getBidderId().equals(UserSession.getInstance().getUsername());

          if (transaction.getTimestamp() > 0) {
              long diff = System.currentTimeMillis() - transaction.getTimestamp();
              boolean isFresh = diff < 5000; // Less than 5 seconds old

              if (isMyBid && isFresh) {
                ToastNotification.show(rootPane,
                    "🎉 Bid placed! You are now the highest bidder at " + priceStr,
                    ToastNotification.Type.SUCCESS);
              } else if (currentHighestBidder.equals(currentUserId) && isFresh) {
                ToastNotification.show(rootPane,
                    "⚠️ You've been outbid! New price: " + priceStr,
                    ToastNotification.Type.WARNING);
              }
          }
          currentHighestBidder = transaction.getBidderId();
          
          // Refresh wallet balance because it might have changed (bid placed or refund received)
          
        });

    BidService.getInstance().setOnAutoBidResult(response -> {
      if ("SUCCESS".equals(response.getStatus())) {
        setupAutoBidBtn.setText("Auto-Bid Active \u2713");
        setupAutoBidBtn.getStyleClass().removeAll("btn-primary", "btn-secondary");
        setupAutoBidBtn.getStyleClass().add("btn-autobid-active");
        ToastNotification.show(rootPane, "Auto-Bid activated!", ToastNotification.Type.SUCCESS);
      } else {
        autoBidError.setText(response.getMessage());
        ToastNotification.show(rootPane, response.getMessage(), ToastNotification.Type.DANGER);
      }
    });

    BidService.getInstance().setOnBidError(msg -> {
      bidError.setText(msg);
      ToastNotification.show(rootPane, msg, ToastNotification.Type.DANGER);
    });

    BidService.getInstance().setOnEndTimeReceived(newEndTime -> {
      long currentNow = System.currentTimeMillis();
      secondsRemaining = (newEndTime > currentNow) ? (int) ((newEndTime - currentNow) / 1000) : 0;
      if (secondsRemaining <= 0) {
        countdownTimer.setText("ENDED");
        if (countdownTimeline != null) {
          countdownTimeline.stop();
        }
        onAuctionEnded();
      } else {
        updateTimerDisplay();
      }
    });

    bidAmountField.textProperty().addListener((obs, old, val) -> bidError.setText(""));
    autoBidIncrementField.setText(String.valueOf(BidService.getInstance().getMinimumIncrement()));
    maxPriceField.textProperty().addListener((obs, old, val) -> autoBidError.setText(""));
    autoBidIncrementField.textProperty().addListener((o, old, val) -> autoBidError.setText(""));

    // ── 2. Set up UI state ──
    totalBids.setText("0");
    totalBidders.setText("0");
    auctionStatus.setText("OPEN");
    auctionStatus.getStyleClass().add("status-open");
    noBidsLabel.setVisible(true);
    currentHighestBidder = "";

    // Set countdown based on UserSession
    long initialEndTime = UserSession.getInstance().getSelectedItemEndTime();
    long now = System.currentTimeMillis();
    secondsRemaining = (initialEndTime > now) ? (int) ((initialEndTime - now) / 1000) : 0;

    startCountdown();

    // ── 3. Request live status and wallet balance AFTER callbacks are wired ──
    
    BidService.getInstance().requestStatus();
    
  }

  private void startCountdown() {
    if (secondsRemaining <= 0) {
      countdownTimer.setText("ENDED");
      onAuctionEnded();
      return;
    }
    if (countdownTimeline != null) {
      countdownTimeline.stop();
    }
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

    countdownTimer.getStyleClass().removeAll("timer-large", "timer-large-urgent");
    if (secondsRemaining < 60) {
      countdownTimer.getStyleClass().add("timer-large-urgent");
    } else {
      countdownTimer.getStyleClass().add("timer-large");
    }
  }

  private void onAuctionEnded() {
    placeBidBtn.setDisable(true);
    bidAmountField.setDisable(true);
    auctionStatus.setText("FINISHED");
    auctionStatus.getStyleClass().removeAll("status-open");
    auctionStatus.getStyleClass().add("status-finished");
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
        setupAutoBidBtn.setText("Auto-Bid Active ✓");
        setupAutoBidBtn.getStyleClass().removeAll("btn-primary", "btn-secondary");
        setupAutoBidBtn.getStyleClass().add("btn-autobid-active");
        autoBidError.setText("");
      }
    } catch (NumberFormatException ex) {
      autoBidError.setText("Invalid numbers");
    }
  }

  @FXML
  private void onAddWatchlist() {
    NetworkClientService.getInstance().sendRequest(
        new Request(com.auction.shared.dto.MessageType.WATCHLIST_ADD, currentUserId, currentAuctionId));
    watchlistBtn.setText("Watching \u2713");
    watchlistBtn.setDisable(true);
    ToastNotification.show(rootPane, "[INFO] Added to Watchlist", ToastNotification.Type.INFO);
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
    UserSession.getInstance().setPendingMyBidsFilter("watching");
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

  private void rebuildBidHistoryUI() {
    bidHistoryList.getChildren().clear();
    final java.util.List<com.auction.shared.models.BidTransaction> history = BidService.getInstance().getBidHistory();
    if (history.isEmpty()) {
      noBidsLabel.setVisible(true);
      noBidsLabel.setManaged(true);
      totalBids.setText("0");
      return;
    }
    noBidsLabel.setVisible(false);
    noBidsLabel.setManaged(false);
    totalBids.setText(String.valueOf(history.size()));

    // Find the highest overall bid in history to mark as winning
    com.auction.shared.models.BidTransaction highestBid = null;
    for (final com.auction.shared.models.BidTransaction tx : history) {
      if (highestBid == null || tx.getBidAmount() > highestBid.getBidAmount()) {
        highestBid = tx;
      }
    }

    // Populate rows (earliest first, which gets added at index 0, pushing them down so latest ends up at the top)
    for (final com.auction.shared.models.BidTransaction tx : history) {
      final String priceStr = String.format("$%.2f", tx.getBidAmount());
      final boolean isHighest = (highestBid != null && tx.getId().equals(highestBid.getId()));
      final boolean isMyBid = tx.getBidderId().equals(currentUserId) 
          || tx.getBidderId().equals(UserSession.getInstance().getUsername());
      
      // Winning badge is shown ONLY on the highest bid if it belongs to the current user
      final String badge = (isHighest && isMyBid) ? "winning" : "";

      final String bidderUsername = (tx.getBidderUsername() != null && !tx.getBidderUsername().isEmpty()) 
          ? tx.getBidderUsername() : tx.getBidderId();
      final String displayName = isMyBid ? UserSession.getInstance().getUsername() : bidderUsername;

      String timeStr = "just now";
      if (tx.getTimestamp() > 0) {
          long diff = System.currentTimeMillis() - tx.getTimestamp();
          if (diff > 3600000) {
              timeStr = (diff / 3600000) + "h ago";
          } else if (diff > 60000) {
              timeStr = (diff / 60000) + "m ago";
          }
      }

      addBidRowToHistory(displayName, priceStr, timeStr, badge);
    }
  }

  private void addBidRowToHistory(final String name, final String price,
      final String time, final String badge) {
    final HBox row = new HBox();
    row.getStyleClass().add("bid-history-row");
    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    row.setSpacing(12);

    final Label nameLbl = new Label(name);
    nameLbl.getStyleClass().add("bid-name");

    final Label priceLbl = new Label(price);
    priceLbl.getStyleClass().add("bid-price");

    final Label timeLbl = new Label(time);
    timeLbl.getStyleClass().add("bid-time");

    row.getChildren().addAll(nameLbl, priceLbl, timeLbl);

    // Spacer to push the badge to the right
    final Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    row.getChildren().add(spacer);

    if (!badge.isEmpty()) {
      final Label bdg = new Label("Winning");
      bdg.getStyleClass().addAll("badge", "badge-success");
      row.getChildren().add(bdg);
    }
    bidHistoryList.getChildren().add(0, row);
  }

  @FXML
  private void onToggleTheme() {
    SceneNavigator.toggleTheme();
  }
}
