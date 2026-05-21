package com.auction.client.controllers;

import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.UserSession;
import com.auction.client.utils.TopNavUtils;
import java.net.URL;
import java.util.ResourceBundle;
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

  private static final String[][] ALL_BIDS = {
    {"Vintage Rolex Watch", "$4,500.00", "$4,500.00", "1h 47m", "winning", "ITEM-006", "COLLECTIBLES", "Original 1969 Rolex Submariner in excellent condition. Serviced 2022.", "4500.00"},
    {"iPhone 15 Pro Max", "$780.00", "$780.00", "32m", "outbid", "ITEM-002", "ELECTRONICS", "Brand new iPhone 15 Pro Max, Natural Titanium, sealed box", "780.00"},
    {"Nike Air Jordan 1", "$1,240.00", "$1,240.00", "Ended", "won", "ITEM-001", "ELECTRONICS", "Asus ROG Strix with RTX 4080, i9, 32GB DDR5", "1240.00"},
    {"Sony WH-1000XM5", "$190.00", "$190.00", "Ended", "lost", "ITEM-003", "ELECTRONICS", "Industry-leading noise cancelling wireless headphones", "190.00"},
    {"MacBook Air M2", "$850.00", "$850.00", "4d 2h", "watching", "ITEM-004", "ELECTRONICS", "Apple MacBook Air M2 chip, 8GB RAM, 256GB SSD, Midnight", "850.00"}
  };

  @Override
  public void initialize(final URL url, final ResourceBundle rb) {
    
    
    
    final String pending = UserSession.getInstance().getPendingMyBidsFilter();
    if ("watching".equals(pending)) {
      UserSession.getInstance().setPendingMyBidsFilter("");
      onFilterWatchlist();
    } else {
      activeFilter = filterAll;
      populateRows(ALL_BIDS);
    }
  }

  @FXML private void onFilterAll() {
    switchFilter(filterAll);
    populateRows(ALL_BIDS);
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

  private String[][] filterBy(final String status) {
    return java.util.Arrays.stream(ALL_BIDS)
        .filter(b -> b[4].equals(status))
        .toArray(String[][]::new);
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

  private void populateRows(final String[][] data) {
    bidsListContainer.getChildren().clear();
    if (data.length == 0) {
      emptyState.setVisible(true);
      emptyState.setManaged(true);
      bidCountLabel.setText("0 bids");
      return;
    }
    emptyState.setVisible(false);
    emptyState.setManaged(false);
    bidCountLabel.setText(data.length + " bid" + (data.length == 1 ? "" : "s"));
    for (String[] bid : data) {
      bidsListContainer.getChildren().add(buildRow(bid));
    }
  }

  private HBox buildRow(final String[] bid) {
    final String title = bid[0];
    final String myBid = bid[1];
    final String curPrice = bid[2];
    final String timeLeft = bid[3];
    final String status = bid[4];

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
    curPriceLbl.setStyle("-fx-font-size: 14px;"); // Slight adjustment for emphasis

    final Label timeLbl = new Label(timeLeft);
    timeLbl.getStyleClass().add("body-small");
    timeLbl.setPrefWidth(120);

    final Label badge = buildBadge(status);
    badge.setPrefWidth(100);
    
    final Button action = buildActionButton(bid);
    action.setPrefWidth(100);

    row.getChildren().addAll(titleLbl, myBidLbl, curPriceLbl, timeLbl, badge, action);
    return row;
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


  private Button buildActionButton(final String[] bid) {
    final String status = bid[4];
    final String itemId = bid[5];
    final String category = bid[6];
    final String desc = bid[7];
    final double price = Double.parseDouble(bid[8]);

    final Button btn = new Button();
    btn.getStyleClass().add("btn-outline");
    btn.setStyle("-fx-font-size: 11px; -fx-padding: 4px 12px;");
    
    switch (status) {
      case "outbid" -> {
        btn.setText("Bid again");
        btn.setOnAction(e -> {
          UserSession.getInstance().setSelectedItemId(itemId);
          UserSession.getInstance().setSelectedAuctionTitle(bid[0]);
          UserSession.getInstance().setSelectedAuctionCategory(category);
          UserSession.getInstance().setSelectedItemDescription(desc);
          UserSession.getInstance().setSelectedItemPrice(price);
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
          UserSession.getInstance().setSelectedItemId(itemId);
          UserSession.getInstance().setSelectedAuctionTitle(bid[0]);
          UserSession.getInstance().setSelectedAuctionCategory(category);
          UserSession.getInstance().setSelectedItemDescription(desc);
          UserSession.getInstance().setSelectedItemPrice(price);
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

  @FXML
  private void onWallet() {
    SceneNavigator.navigateTo(SceneNavigator.View.WALLET);
  }
}
