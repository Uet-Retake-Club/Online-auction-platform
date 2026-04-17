package com.auction.client.controllers;

import com.auction.client.utils.SceneNavigator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * AuctionDetailController.java
 * ─────────────────────────────────────────
 * Xử lý màn hình chi tiết đấu giá (AuctionDetailView.fxml).
 *
 * Chức năng:
 *  - Hiển thị thông tin sản phẩm, giá hiện tại, số lượt đặt giá
 *  - Đồng hồ đếm ngược cập nhật mỗi giây bằng JavaFX Timeline
 *  - Validate và đặt giá (placeBid)
 *  - Hiển thị lịch sử đặt giá (bidHistoryList)
 *  - Đổi màu timer theo thời gian còn lại
 */
public class AuctionDetailController implements Initializable {

    // ── FXML nodes ───────────────────────────────────────────
    @FXML private Label    userLabel;
    @FXML private Label    backLabel;
    @FXML private Label    itemTitle;
    @FXML private Label    itemMeta;
    @FXML private Label    itemDescription;
    @FXML private Label    currentPrice;
    @FXML private Label    totalBids;
    @FXML private Label    totalBidders;
    @FXML private Label    auctionStatus;
    @FXML private Label    countdownTimer;
    @FXML private Label    minBidHint;
    @FXML private TextField bidAmountField;
    @FXML private Label    bidError;
    @FXML private Button   placeBidBtn;
    @FXML private Button   watchlistBtn;
    @FXML private VBox     bidHistoryList;
    @FXML private Label    noBidsLabel;

    // ── Dummy state (replace with real Auction object later) ─
    private double currentBidAmount = 1240.00;
    private double minimumIncrement = 20.00;
    private int    secondsRemaining = 6452; // ~1h 47m 32s
    private Timeline countdownTimeline;

    // ── Dummy bid history ─────────────────────────────────────
    private static final String[][] DUMMY_BIDS = {
        { "user_alpha",    "$1,240.00", "2 min ago",  "winning" },
        { "buyer_99",      "$1,180.00", "8 min ago",  "" },
        { "collector_vn",  "$1,050.00", "22 min ago", "" },
        { "techbid2025",   "$950.00",   "35 min ago", "" },
        { "vintage_lover", "$800.00",   "1 hr ago",   "" },
    };

    // ── Lifecycle ────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userLabel.setText("JD"); // TODO: UserSession.getInstance().getInitials()

        // Populate dummy data — replace with real Auction object later
        itemTitle.setText("Vintage Rolex Submariner 1969");
        itemMeta.setText("Collectibles  ·  Seller: watch_king99");
        itemDescription.setText(
            "Original 1969 Rolex Submariner in excellent condition. " +
            "Original bracelet, box, and papers included. Serviced in 2022. " +
            "Running perfectly with minor surface scratches consistent with age."
        );
        updatePrice(currentBidAmount);
        totalBids.setText("14");
        totalBidders.setText("7");
        auctionStatus.setText("OPEN");

        // Start countdown timer
        startCountdown();

        // Populate bid history
        loadBidHistory();

        // Clear error on typing
        bidAmountField.textProperty().addListener(
            (obs, old, val) -> bidError.setText(""));
    }

    // ── Countdown timer ──────────────────────────────────────

    /**
     * Khởi động Timeline cập nhật đồng hồ mỗi 1 giây.
     * Đổi màu timer: xanh → vàng (< 10 phút) → đỏ (< 1 phút).
     * TODO: kết nối với thời gian thực từ server.
     */
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
            })
        );
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
        updateTimerDisplay();
    }

    private void updateTimerDisplay() {
        int h = secondsRemaining / 3600;
        int m = (secondsRemaining % 3600) / 60;
        int s = secondsRemaining % 60;
        countdownTimer.setText(String.format("%02d:%02d:%02d", h, m, s));

        // Đổi màu theo mức độ khẩn cấp
        if (secondsRemaining < 60) {
            // Đỏ — dưới 1 phút
            countdownTimer.setStyle(
                "-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#E53238;-fx-font-family:'Segoe UI';");
        } else if (secondsRemaining < 600) {
            // Vàng — dưới 10 phút
            countdownTimer.setStyle(
                "-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#F5A623;-fx-font-family:'Segoe UI';");
        } else {
            // Bình thường
            countdownTimer.setStyle(
                "-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#F5A623;-fx-font-family:'Segoe UI';");
        }
    }

    private void onAuctionEnded() {
        placeBidBtn.setDisable(true);
        bidAmountField.setDisable(true);
        auctionStatus.setText("FINISHED");
        auctionStatus.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#E53238;");
    }

    // ── Place bid ────────────────────────────────────────────

    @FXML
    private void onPlaceBid() {
        String input = bidAmountField.getText().trim();

        // Validate
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
        double minBid = currentBidAmount + minimumIncrement;
        if (amount < minBid) {
            bidError.setText(String.format("Minimum bid is $%.2f", minBid));
            return;
        }

        // TODO: replace with BidService.placeBid(auctionId, amount)
        // BidService.placeBid(auctionId, amount)
        //   .onSuccess(bid -> Platform.runLater(() -> {
        //       updatePrice(bid.getAmount());
        //       addBidRow(bid);
        //   }))
        //   .onFailure(err -> bidError.setText(err.getMessage()));

        // Simulate success for UI demo
        updatePrice(amount);
        addBidRowToHistory("You", String.format("$%.2f", amount),
                           "just now", "winning");
        bidAmountField.clear();
        bidError.setText("");
        noBidsLabel.setVisible(false);
        noBidsLabel.setManaged(false);
    }

    // ── Watchlist ────────────────────────────────────────────

    @FXML
    private void onAddWatchlist() {
        watchlistBtn.setText("Added to watchlist");
        watchlistBtn.setDisable(true);
        // TODO: WatchlistService.add(auctionId)
    }

    // ── Navigation ───────────────────────────────────────────

    @FXML private void onBack()      { SceneNavigator.navigateTo(SceneNavigator.View.HOME); }
    @FXML private void onHome()      { SceneNavigator.navigateTo(SceneNavigator.View.HOME); }
    @FXML private void onMyBids()    { SceneNavigator.navigateTo(SceneNavigator.View.MY_BIDS); }
    @FXML private void onWatchlist() { SceneNavigator.navigateTo(SceneNavigator.View.MY_BIDS); }
    @FXML private void onSell()      { SceneNavigator.navigateTo(SceneNavigator.View.CREATE_LISTING); }
    @FXML private void onProfile()   { System.out.println("TODO: ProfileView"); }

    // ── Helpers ──────────────────────────────────────────────

    private void updatePrice(double amount) {
        currentBidAmount = amount;
        currentPrice.setText(String.format("$%.2f", amount));
        minBidHint.setText(String.format("Minimum bid: $%.2f", amount + minimumIncrement));
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

    /**
     * Tạo một hàng lịch sử đặt giá dạng HBox và thêm vào danh sách.
     * TODO: khi có dữ liệu thật, thay String[] bằng BidTransaction object.
     */
    private void addBidRowToHistory(String name, String price,
                                    String time, String badge) {
        HBox row = new HBox();
        row.setStyle("-fx-border-color:transparent transparent #F4F4F4 transparent;" +
                     "-fx-border-width:0 0 1px 0;-fx-padding:8px 0;-fx-alignment:CENTER_LEFT;");
        row.setSpacing(8);

        Label nameLbl = new Label(name);
        nameLbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#333;");
        HBox.setHgrow(nameLbl, Priority.ALWAYS);

        Label priceLbl = new Label(price);
        priceLbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#E53238;-fx-min-width:90px;");

        Label timeLbl = new Label(time);
        timeLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#AAAAAA;-fx-min-width:80px;");

        row.getChildren().addAll(nameLbl, priceLbl, timeLbl);

        if (!badge.isEmpty()) {
            Label bdg = new Label("Winning");
            bdg.setStyle("-fx-background-color:#EAF5EA;-fx-text-fill:#5BA55B;" +
                         "-fx-font-size:10px;-fx-font-weight:bold;" +
                         "-fx-padding:2px 8px;-fx-background-radius:10px;");
            row.getChildren().add(bdg);
        }

        bidHistoryList.getChildren().add(0, row); // newest on top
    }
}