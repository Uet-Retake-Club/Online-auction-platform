package com.auction.client.controllers;

import com.auction.client.utils.SceneNavigator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.net.URL;
import java.util.ResourceBundle;

public class MyBidsController implements Initializable {

    @FXML private Label    userLabel;
    @FXML private Label    bidCountLabel;
    @FXML private VBox     bidsListContainer;
    @FXML private VBox     emptyState;
    @FXML private Button   filterAll;
    @FXML private Button   filterWinning;
    @FXML private Button   filterOutbid;
    @FXML private Button   filterWon;
    @FXML private Button   filterWatchlist;

    private Button activeFilter;

    private static final String[][] ALL_BIDS = {
        { "Vintage Rolex Watch",  "$1,240.00", "$1,240.00", "1h 47m",  "winning"  },
        { "iPhone 15 Pro Max",    "$720.00",   "$780.00",   "32m",     "outbid"   },
        { "Nike Air Jordan 1",    "$210.00",   "$210.00",   "Ended",   "won"      },
        { "Sony WH-1000XM5",      "$170.00",   "$190.00",   "Ended",   "lost"     },
        { "MacBook Air M2",       "$820.00",   "$820.00",   "4d 2h",   "watching" },
    };

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userLabel.setText("JD");
        activeFilter = filterAll;
        populateRows(ALL_BIDS);
    }

    @FXML private void onFilterAll() { switchFilter(filterAll); populateRows(ALL_BIDS); }
    @FXML private void onFilterWinning() { switchFilter(filterWinning); populateRows(filterBy("winning")); }
    @FXML private void onFilterOutbid() { switchFilter(filterOutbid); populateRows(filterBy("outbid")); }
    @FXML private void onFilterWon() { switchFilter(filterWon); populateRows(filterBy("won")); }
    @FXML private void onFilterWatchlist() { switchFilter(filterWatchlist); populateRows(filterBy("watching")); }

    private void switchFilter(Button btn) {
        if (activeFilter != null) {
            activeFilter.getStyleClass().remove("nav-item-active");
            activeFilter.getStyleClass().add("nav-item");
        }
        btn.getStyleClass().remove("nav-item");
        btn.getStyleClass().add("nav-item-active");
        activeFilter = btn;
    }

    private String[][] filterBy(String status) {
        return java.util.Arrays.stream(ALL_BIDS).filter(b -> b[4].equals(status)).toArray(String[][]::new);
    }

    // ── ĐIỀU HƯỚNG ───────────────────────────────────────────
    @FXML private void onHome()    { SceneNavigator.navigateTo(SceneNavigator.View.HOME); }
    @FXML private void onSell()    { SceneNavigator.navigateTo(SceneNavigator.View.CREATE_LISTING); }
    @FXML private void onProfile() { System.out.println("TODO: ProfileView"); }
    @FXML private void onLogout()  { SceneNavigator.navigateTo(SceneNavigator.View.LOGIN); }

    private void populateRows(String[][] data) {
        bidsListContainer.getChildren().clear();
        if (data.length == 0) {
            emptyState.setVisible(true); emptyState.setManaged(true); bidCountLabel.setText("0 bids"); return;
        }
        emptyState.setVisible(false); emptyState.setManaged(false);
        bidCountLabel.setText(data.length + " bid" + (data.length == 1 ? "" : "s"));
        for (String[] bid : data) {
            bidsListContainer.getChildren().add(buildRow(bid[0], bid[1], bid[2], bid[3], bid[4]));
        }
    }

    private HBox buildRow(String title, String myBid, String curPrice, String timeLeft, String status) {
        HBox row = new HBox();
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color:transparent transparent #F4F4F4 transparent;-fx-border-width:0 0 1px 0;-fx-padding:10px 14px;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#111;");
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        Label myBidLbl = new Label(myBid);
        myBidLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#333;"); myBidLbl.setPrefWidth(100);

        Label curPriceLbl = new Label(curPrice);
        curPriceLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#E53238;"); curPriceLbl.setPrefWidth(120);

        Label timeLbl = new Label(timeLeft);
        timeLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#888;"); timeLbl.setPrefWidth(100);

        Label badge = buildBadge(status); badge.setPrefWidth(90);
        Button action = buildActionButton(status); action.setPrefWidth(100);

        row.getChildren().addAll(titleLbl, myBidLbl, curPriceLbl, timeLbl, badge, action);
        return row;
    }

    private Label buildBadge(String status) {
        Label b = new Label();
        switch (status) {
            case "winning"  -> { b.setText("Winning");  b.setStyle(badgeStyle("#EAF5EA","#5BA55B")); }
            case "outbid"   -> { b.setText("Outbid");   b.setStyle(badgeStyle("#FEF6E6","#F5A623")); }
            case "won"      -> { b.setText("Won");      b.setStyle(badgeStyle("#EAF5EA","#5BA55B")); }
            case "lost"     -> { b.setText("Lost");     b.setStyle(badgeStyle("#F4F4F4","#888888")); }
            case "watching" -> { b.setText("Watching"); b.setStyle(badgeStyle("#E8F0FE","#1A73E8")); }
            default         -> b.setText(status);
        }
        return b;
    }

    private String badgeStyle(String bg, String fg) {
        return "-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:3px 10px;-fx-background-radius:10px;";
    }

    private Button buildActionButton(String status) {
        Button btn = new Button();
        btn.setStyle("-fx-background-color:white;-fx-border-color:#E0E0E0;-fx-border-width:1px;-fx-border-radius:5px;-fx-background-radius:5px;-fx-font-size:11px;-fx-padding:4px 12px;-fx-cursor:hand;-fx-effect:null;");
        switch (status) {
            case "outbid"   -> {
                btn.setText("Bid again");
                btn.setOnAction(e -> SceneNavigator.navigateTo(SceneNavigator.View.AUCTION_DETAIL));
            }
            case "won"      -> {
                btn.setText("Pay now");
                btn.setStyle("-fx-background-color:#E53238;-fx-text-fill:white;-fx-font-weight:bold;-fx-border-color:transparent;-fx-border-radius:5px;-fx-background-radius:5px;-fx-font-size:11px;-fx-padding:4px 12px;-fx-cursor:hand;-fx-effect:null;");
                btn.setOnAction(e -> System.out.println("TODO: payment flow"));
            }
            default         -> {
                btn.setText("View");
                btn.setOnAction(e -> SceneNavigator.navigateTo(SceneNavigator.View.AUCTION_DETAIL));
            }
        }
        return btn;
    }
}