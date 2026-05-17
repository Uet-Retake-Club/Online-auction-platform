package com.auction.client.controllers;

import com.auction.client.services.NetworkClientService;
import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.UserSession;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.MyBidItemDTO;
import com.auction.shared.dto.Request;
import com.google.gson.Gson;
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
 * MyBidsController handles the My Bids view.
 * Fetches the user's bids and watchlist items from the server and displays them live.
 */
public class MyBidsController implements Initializable {

  @FXML private Label userLabel;
  @FXML private Label bidCountLabel;
  @FXML private VBox bidsListContainer;
  @FXML private VBox emptyState;
  @FXML private Button filterAll;
  @FXML private Button filterWinning;
  @FXML private Button filterOutbid;
  @FXML private Button filterWon;
  @FXML private Button filterWatchlist;

  private Button activeFilter;
  private final Gson gson = new Gson();
  private final List<MyBidItemDTO> allMyBids = new ArrayList<>();

  @Override
  public void initialize(final URL url, final ResourceBundle rb) {
    userLabel.setText(UserSession.getInstance().getInitials());
    fetchMyBidsFromServer();
  }

  /**
   * Fetches real user bid transactions and watchlist records from the server.
   */
  private void fetchMyBidsFromServer() {
    bidsListContainer.getChildren().clear();
    bidCountLabel.setText("Loading...");

    final Request req = new Request(MessageType.GET_MY_BIDS,
        UserSession.getInstance().getUserId(), "");

    // One-shot listener to receive the user's bids
    final NetworkClientService.ServerMessageListener[] ref =
        new NetworkClientService.ServerMessageListener[1];
    ref[0] = response -> {
      if (response.getType() == MessageType.GET_MY_BIDS_RESPONSE) {
        NetworkClientService.getInstance().removeListener(ref[0]);

        final List<MyBidItemDTO> items = new ArrayList<>();
        try {
          final MyBidItemDTO[] arr = gson.fromJson(response.getPayload(), MyBidItemDTO[].class);
          if (arr != null) {
            for (final MyBidItemDTO el : arr) {
              items.add(el);
            }
          }
        } catch (Exception ignored) { }

        Platform.runLater(() -> {
          allMyBids.clear();
          allMyBids.addAll(items);
          
          final String pending = UserSession.getInstance().getPendingMyBidsFilter();
          if ("watching".equals(pending)) {
            UserSession.getInstance().setPendingMyBidsFilter("");
            switchFilter(filterWatchlist);
            populateRows(filterBy("watching"));
          } else {
            switchFilter(filterAll);
            populateRows(allMyBids);
          }
        });
      }
    };
    NetworkClientService.getInstance().addListener(ref[0]);
    NetworkClientService.getInstance().sendRequest(req);
  }

  @FXML private void onFilterAll() {
    switchFilter(filterAll);
    populateRows(allMyBids);
  }

  @FXML private void onFilterWinning() {
    switchFilter(filterWinning);
    populateRows(filterBy("winning"));
  }

  @FXML private void onFilterOutbid() {
    switchFilter(filterOutbid);
    populateRows(filterBy("outbid"));
  }

  @FXML private void onFilterWon() {
    switchFilter(filterWon);
    populateRows(filterBy("won"));
  }

  @FXML private void onFilterWatchlist() {
    switchFilter(filterWatchlist);
    populateRows(filterBy("watching"));
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

  private List<MyBidItemDTO> filterBy(final String status) {
    return allMyBids.stream()
        .filter(b -> b.status.equalsIgnoreCase(status))
        .toList();
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

  private void populateRows(final List<MyBidItemDTO> data) {
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
    for (MyBidItemDTO bid : data) {
      bidsListContainer.getChildren().add(buildRow(bid));
    }
  }

  private HBox buildRow(final MyBidItemDTO bid) {
    final HBox row = new HBox();
    row.getStyleClass().add("table-row");

    final Label titleLbl = new Label(bid.name);
    titleLbl.getStyleClass().add("label-bold");
    HBox.setHgrow(titleLbl, Priority.ALWAYS);
    titleLbl.setMaxWidth(Double.MAX_VALUE);

    final String myBidStr = bid.myBidAmount > 0 ? String.format("$%,.2f", bid.myBidAmount) : "-";
    final Label myBidLbl = new Label(myBidStr);
    myBidLbl.getStyleClass().add("body-text");
    myBidLbl.setPrefWidth(120);

    final String curPriceStr = String.format("$%,.2f", bid.currentPrice);
    final Label curPriceLbl = new Label(curPriceStr);
    curPriceLbl.getStyleClass().addAll("label-bold", "price-tag");
    curPriceLbl.setPrefWidth(140);
    curPriceLbl.setStyle("-fx-font-size: 14px;");

    final Label timeLbl = new Label(formatTimeLeft(bid.endTime));
    timeLbl.getStyleClass().add("body-small");
    timeLbl.setPrefWidth(120);

    final Label badge = buildBadge(bid.status);
    badge.setPrefWidth(100);
    
    final Button action = buildActionButton(bid);
    action.setPrefWidth(100);

    row.getChildren().addAll(titleLbl, myBidLbl, curPriceLbl, timeLbl, badge, action);
    return row;
  }

  private String formatTimeLeft(final long endTime) {
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
    switch (status.toLowerCase()) {
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

  private Button buildActionButton(final MyBidItemDTO bid) {
    final Button btn = new Button();
    btn.getStyleClass().add("btn-outline");
    btn.setStyle("-fx-font-size: 11px; -fx-padding: 4px 12px;");
    
    switch (bid.status.toLowerCase()) {
      case "outbid" -> {
        btn.setText("Bid again");
        btn.setOnAction(e -> {
          UserSession.getInstance().setSelectedItemId(bid.itemId);
          UserSession.getInstance().setSelectedAuctionTitle(bid.name);
          UserSession.getInstance().setSelectedAuctionCategory(bid.category);
          UserSession.getInstance().setSelectedItemDescription(bid.description);
          UserSession.getInstance().setSelectedItemPrice(bid.currentPrice);
          SceneNavigator.navigateTo(SceneNavigator.View.AUCTION_DETAIL);
        });
      }
      case "won" -> {
        btn.setText("Pay now");
        btn.getStyleClass().removeAll("btn-outline");
        btn.getStyleClass().add("btn-primary");
        btn.setOnAction(e -> System.out.println("TODO: payment flow"));
      }
      default -> {
        btn.setText("View");
        btn.setOnAction(e -> {
          UserSession.getInstance().setSelectedItemId(bid.itemId);
          UserSession.getInstance().setSelectedAuctionTitle(bid.name);
          UserSession.getInstance().setSelectedAuctionCategory(bid.category);
          UserSession.getInstance().setSelectedItemDescription(bid.description);
          UserSession.getInstance().setSelectedItemPrice(bid.currentPrice);
          SceneNavigator.navigateTo(SceneNavigator.View.AUCTION_DETAIL);
        });
      }
    }
    return btn;
  }

  @FXML
  private void onToggleTheme() {
    SceneNavigator.toggleTheme();
  }
}