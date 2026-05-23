package com.auction.client.controllers;

import com.auction.client.services.NetworkClientService;
import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.UserSession;
import com.auction.client.utils.TopNavUtils;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.shared.dto.UserBidDTO;
import com.google.gson.Gson;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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
 * MyBidsController handles the My Bids view.
 */
public class MyBidsController implements Initializable {

  @FXML private Label bidCountLabel;
  @FXML private VBox bidsListContainer;
  @FXML private VBox emptyState;
  @FXML private Button filterAll;
  @FXML private Button filterWinning;
  @FXML private Button filterOutbid;
  @FXML private Button filterWon;
  @FXML private Button filterWatchlist;

  private Button activeFilter;
  private final List<UserBidDTO> cachedUserBids = new ArrayList<>();
  private String currentFilter = "all"; // "all", "winning", "outbid", "won", "watching"
  private final Gson gson = new Gson();

  @Override
  public void initialize(final URL url, final ResourceBundle rb) {
    activeFilter = filterAll;
    fetchUserBids();
  }

  private void fetchUserBids() {
    bidsListContainer.getChildren().clear();
    bidCountLabel.setText("Loading...");

    final Request req = new Request(MessageType.GET_MY_BIDS,
        UserSession.getInstance().getUserId(), "");

    final NetworkClientService.ServerMessageListener[] ref =
        new NetworkClientService.ServerMessageListener[1];
    ref[0] = response -> {
      if (response.getType() == MessageType.MY_BIDS_RESPONSE) {
        NetworkClientService.getInstance().removeListener(ref[0]);

        final List<UserBidDTO> bids = new ArrayList<>();
        try {
          final UserBidDTO[] arr = gson.fromJson(response.getPayload(), UserBidDTO[].class);
          if (arr != null) {
            bids.addAll(Arrays.asList(arr));
          }
        } catch (Exception e) {
          e.printStackTrace();
        }

        Platform.runLater(() -> {
          cachedUserBids.clear();
          cachedUserBids.addAll(bids);
          
          final String pending = UserSession.getInstance().getPendingMyBidsFilter();
          if ("watching".equals(pending)) {
            UserSession.getInstance().setPendingMyBidsFilter("");
            onFilterWatchlist();
          } else {
            applyFilter();
          }
        });
      }
    };
    NetworkClientService.getInstance().addListener(ref[0]);
    NetworkClientService.getInstance().sendRequest(req);
  }

  @FXML private void onFilterAll() {
    switchFilter(filterAll);
    currentFilter = "all";
    applyFilter();
  }

  @FXML private void onFilterWinning() {
    switchFilter(filterWinning);
    currentFilter = "winning";
    applyFilter();
  }

  @FXML private void onFilterOutbid() {
    switchFilter(filterOutbid);
    currentFilter = "outbid";
    applyFilter();
  }

  @FXML private void onFilterWon() {
    switchFilter(filterWon);
    currentFilter = "won";
    applyFilter();
  }

  @FXML private void onFilterWatchlist() {
    switchFilter(filterWatchlist);
    currentFilter = "watching";
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
    final String currentUserId = UserSession.getInstance().getUserId();
    final List<UserBidDTO> filtered = new ArrayList<>();

    for (final UserBidDTO dto : cachedUserBids) {
      final String displayStatus = determineDisplayStatus(dto, currentUserId);
      switch (currentFilter) {
        case "all" -> filtered.add(dto);
        case "winning" -> {
          if ("winning".equals(displayStatus)) filtered.add(dto);
        }
        case "outbid" -> {
          if ("outbid".equals(displayStatus)) filtered.add(dto);
        }
        case "won" -> {
          if ("won".equals(displayStatus)) filtered.add(dto);
        }
        case "watching" -> {
          if (dto.isWatchlisted()) filtered.add(dto);
        }
      }
    }

    populateRows(filtered);
  }

  private String determineDisplayStatus(final UserBidDTO dto, final String currentUserId) {
    if (dto.getMyHighestBid() <= 0.0 && dto.isWatchlisted()) {
      return "watching";
    }
    if ("OPEN".equals(dto.getStatus())) {
      if (currentUserId.equals(dto.getHighestBidderId())) {
        return "winning";
      } else {
        return "outbid";
      }
    } else {
      if (currentUserId.equals(dto.getHighestBidderId())) {
        return "won";
      } else {
        return "lost";
      }
    }
  }

  private void populateRows(final List<UserBidDTO> data) {
    bidsListContainer.getChildren().clear();
    if (data.isEmpty()) {
      emptyState.setVisible(true);
      emptyState.setManaged(true);
      bidCountLabel.setText("0 bids");
      return;
    }
    emptyState.setVisible(false);
    emptyState.setManaged(false);
    bidCountLabel.setText(data.size() + " bid" + (data.size() == 1 ? "" : "s"));
    
    final String currentUserId = UserSession.getInstance().getUserId();
    for (final UserBidDTO dto : data) {
      final String displayStatus = determineDisplayStatus(dto, currentUserId);
      bidsListContainer.getChildren().add(buildRow(dto, displayStatus));
    }
  }

  private HBox buildRow(final UserBidDTO dto, final String displayStatus) {
    final String title = dto.getName();
    final String myBid = dto.getMyHighestBid() > 0 ? String.format("$%.2f", dto.getMyHighestBid()) : "-";
    final String curPrice = String.format("$%.2f", dto.getCurrentHighestBid());
    final String timeLeft = formatTimeLeft(dto.getEndTime(), dto.getStatus());

    final HBox row = new HBox();
    row.getStyleClass().add("table-row");

    final Label titleLbl = new Label(title);
    titleLbl.getStyleClass().add("label-bold");
    HBox.setHgrow(titleLbl, Priority.ALWAYS);
    titleLbl.setMaxWidth(Double.MAX_VALUE);

    final Label myBidLbl = new Label(myBid);
    myBidLbl.getStyleClass().add("body-text");
    myBidLbl.setPrefWidth(120);

    final Label curPriceLbl = new Label(curPrice);
    curPriceLbl.getStyleClass().addAll("label-bold", "price-tag");
    curPriceLbl.setPrefWidth(140);
    curPriceLbl.setStyle("-fx-font-size: 14px;");

    final Label timeLbl = new Label(timeLeft);
    timeLbl.getStyleClass().add("body-small");
    timeLbl.setPrefWidth(120);

    final Label badge = buildBadge(displayStatus);
    badge.setPrefWidth(100);
    
    final Button action = buildActionButton(dto, displayStatus);
    action.setPrefWidth(100);

    row.getChildren().addAll(titleLbl, myBidLbl, curPriceLbl, timeLbl, badge, action);
    return row;
  }

  private String formatTimeLeft(final long endTime, final String status) {
    if ("FINISHED".equals(status) || "PAID".equals(status) || "CANCELED".equals(status)) {
      return "Ended";
    }
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
      case "winning" -> {
        b.setText("Winning");
        b.getStyleClass().add("badge-success");
      }
      case "outbid" -> {
        b.setText("Outbid");
        b.getStyleClass().add("badge-warning");
      }
      case "won" -> {
        b.setText("Won");
        b.getStyleClass().add("badge-success");
      }
      case "lost" -> {
        b.setText("Lost");
        b.getStyleClass().add("badge-danger");
      }
      case "watching" -> {
        b.setText("Watching");
        b.getStyleClass().add("badge-info");
      }
      default -> b.setText(status);
    }
    return b;
  }

  private Button buildActionButton(final UserBidDTO dto, final String displayStatus) {
    final String itemId = dto.getItemId();
    final String category = dto.getCategory();
    final String desc = dto.getDescription();
    final double price = dto.getCurrentHighestBid() > 0 ? dto.getCurrentHighestBid() : dto.getStartingPrice();

    final Button btn = new Button();
    btn.getStyleClass().add("btn-outline");
    btn.setStyle("-fx-font-size: 11px; -fx-padding: 4px 12px;");
    
    switch (displayStatus) {
      case "outbid" -> {
        btn.setText("Bid again");
        btn.setOnAction(e -> {
          UserSession.getInstance().setSelectedItemId(itemId);
          UserSession.getInstance().setSelectedAuctionTitle(dto.getName());
          UserSession.getInstance().setSelectedAuctionCategory(category);
          UserSession.getInstance().setSelectedItemDescription(desc);
          UserSession.getInstance().setSelectedItemPrice(price);
          UserSession.getInstance().setSelectedItemEndTime(dto.getEndTime());
          SceneNavigator.navigateTo(SceneNavigator.View.AUCTION_DETAIL);
        });
      }
      case "won" -> {
        btn.setText("Pay now");
        btn.getStyleClass().removeAll("btn-outline");
        btn.getStyleClass().add("btn-primary");
        btn.setOnAction(e -> System.out.println("TODO: payment flow for " + itemId));
      }
      default -> {
        btn.setText("View");
        btn.setOnAction(e -> {
          UserSession.getInstance().setSelectedItemId(itemId);
          UserSession.getInstance().setSelectedAuctionTitle(dto.getName());
          UserSession.getInstance().setSelectedAuctionCategory(category);
          UserSession.getInstance().setSelectedItemDescription(desc);
          UserSession.getInstance().setSelectedItemPrice(price);
          UserSession.getInstance().setSelectedItemEndTime(dto.getEndTime());
          SceneNavigator.navigateTo(SceneNavigator.View.AUCTION_DETAIL);
        });
      }
    }
    return btn;
  }

  @FXML
  private void onHome() {
    SceneNavigator.navigateTo(SceneNavigator.View.HOME);
  }

  @FXML
  private void onSell() {
    SceneNavigator.navigateTo(SceneNavigator.View.SELLER);
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

  @FXML
  private void onWallet() {
    SceneNavigator.navigateTo(SceneNavigator.View.WALLET);
  }
}
