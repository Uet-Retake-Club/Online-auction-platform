package com.auction.client.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.UserSession;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * AdminController.java
 * ─────────────────────────────────────────
 * Xử lý màn hình quản trị (AdminView.fxml).
 *
 * Chức năng:
 * - Dashboard: thống kê tổng quan (users, auctions, bids, revenue)
 * - Bảng người dùng với nút Edit / Ban
 * - Bảng phiên đấu giá với nút Cancel / View
 * - Tab navigation giữa Dashboard / Users / Auctions / Bids
 */
public class AdminController implements Initializable {

    // ── FXML nodes ───────────────────────────────────────────
    @FXML
    private Label adminLabel;
    @FXML
    private Label statTotalUsers;
    @FXML
    private Label statActiveAuctions;
    @FXML
    private Label statTotalBids;
    @FXML
    private Label statRevenue;
    @FXML
    private VBox usersTableContainer;
    @FXML
    private VBox auctionsTableContainer;
    @FXML
    private VBox dashboardTab;
    @FXML
    private VBox tabPlaceholder;
    @FXML
    private Button navDashboard;
    @FXML
    private Button navUsers;
    @FXML
    private Button navAuctions;
    @FXML
    private Button navBids;

    private Button activeNav;

    // ── Dummy users ───────────────────────────────────────────
    private static final String[][] DUMMY_USERS = {
            { "user_alpha", "alpha@mail.com", "Bidder", "Active" },
            { "watch_king99", "king@mail.com", "Seller", "Active" },
            { "collector_vn", "col@mail.com", "Bidder", "Suspended" },
            { "techbid2025", "tech@mail.com", "Bidder", "Active" },
            { "admin_root", "admin@hub.com", "Admin", "Active" },
    };

    // ── Dummy auctions ────────────────────────────────────────
    private static final String[][] DUMMY_AUCTIONS = {
            { "Vintage Rolex Submariner", "watch_king99", "$1,240.00", "RUNNING" },
            { "iPhone 15 Pro Max", "techbid2025", "$780.00", "RUNNING" },
            { "Nike Air Jordan 1", "collector_vn", "$210.00", "FINISHED" },
            { "MacBook Air M2", "user_alpha", "$820.00", "OPEN" },
    };

    // ── Lifecycle ────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        adminLabel.setText(UserSession.getInstance().getDisplayName());
        activeNav = navDashboard;

        // Load stats
        statTotalUsers.setText("1,284");
        statActiveAuctions.setText("342");
        statTotalBids.setText("8,741");
        statRevenue.setText("$94,200");

        // Populate tables
        loadUsersTable(DUMMY_USERS);
        loadAuctionsTable(DUMMY_AUCTIONS);
    }

    // ── Tab navigation ────────────────────────────────────────

    @FXML
    private void onTabDashboard() {
        switchNav(navDashboard);
        dashboardTab.setVisible(true);
        dashboardTab.setManaged(true);
        tabPlaceholder.setVisible(false);
        tabPlaceholder.setManaged(false);
    }

    @FXML
    private void onTabUsers() {
        switchNav(navUsers);
        showPlaceholder("Users management — coming soon");
    }

    @FXML
    private void onTabAuctions() {
        switchNav(navAuctions);
        showPlaceholder("Auctions management — coming soon");
    }

    @FXML
    private void onTabBids() {
        switchNav(navBids);
        showPlaceholder("Bids log — coming soon");
    }

    private void switchNav(Button btn) {
        if (activeNav != null) {
            activeNav.getStyleClass().remove("nav-item-active");
            activeNav.getStyleClass().add("nav-item");
        }
        btn.getStyleClass().remove("nav-item");
        btn.getStyleClass().add("nav-item-active");
        activeNav = btn;
    }

    private void showPlaceholder(String msg) {
        dashboardTab.setVisible(false);
        dashboardTab.setManaged(false);
        tabPlaceholder.setVisible(true);
        tabPlaceholder.setManaged(true);
        ((Label) tabPlaceholder.getChildren().get(0)).setText(msg);
    }

    // ── Sign out ──────────────────────────────────────────────

    @FXML
    private void onLogout() {
        UserSession.getInstance().clear();
        SceneNavigator.navigateTo(SceneNavigator.View.LOGIN);
    }

    // ── Table builders ────────────────────────────────────────

    private void loadUsersTable(String[][] users) {
        usersTableContainer.getChildren().clear();
        for (String[] u : users) {
            usersTableContainer.getChildren().add(
                    buildUserRow(u[0], u[1], u[2], u[3]));
        }
    }

    private HBox buildUserRow(String username, String email,
            String role, String status) {
        HBox row = new HBox();
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color:transparent transparent #F4F4F4 transparent;" +
                "-fx-border-width:0 0 1px 0;-fx-padding:9px 16px;");

        Label uLbl = new Label(username);
        uLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#111;");
        HBox.setHgrow(uLbl, Priority.ALWAYS);

        Label eLbl = new Label(email);
        eLbl.setPrefWidth(200);
        eLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#555;");

        Label roleLbl = buildSmallBadge(role,
                role.equals("Admin") ? "#F4F4F4:#555555"
                        : role.equals("Seller") ? "#F4F4F4:#555555" : "#E8F0FE:#1A73E8");
        roleLbl.setPrefWidth(90);

        Label statusLbl = buildSmallBadge(status,
                status.equals("Active") ? "#EAF5EA:#5BA55B" : "#FEF6E6:#F5A623");
        statusLbl.setPrefWidth(90);

        // Edit button
        Button editBtn = new Button("Edit");
        editBtn.setStyle(smallBtnStyle(false));
        editBtn.setOnAction(e -> System.out.println("TODO: edit user " + username));

        // Ban/Unban button
        boolean isBanned = status.equals("Suspended");
        Button banBtn = new Button(isBanned ? "Unban" : "Ban");
        banBtn.setStyle(smallBtnStyle(true));
        banBtn.setOnAction(e -> System.out.println("TODO: toggle ban " + username));

        HBox actions = new HBox(6, editBtn, banBtn);
        actions.setPrefWidth(120);

        row.getChildren().addAll(uLbl, eLbl, roleLbl, statusLbl, actions);
        return row;
    }

    private void loadAuctionsTable(String[][] auctions) {
        auctionsTableContainer.getChildren().clear();
        for (String[] a : auctions) {
            auctionsTableContainer.getChildren().add(
                    buildAuctionRow(a[0], a[1], a[2], a[3]));
        }
    }

    private HBox buildAuctionRow(String title, String seller,
            String price, String status) {
        HBox row = new HBox();
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color:transparent transparent #F4F4F4 transparent;" +
                "-fx-border-width:0 0 1px 0;-fx-padding:9px 16px;");

        Label tLbl = new Label(title);
        tLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#111;");
        HBox.setHgrow(tLbl, Priority.ALWAYS);

        Label sLbl = new Label(seller);
        sLbl.setPrefWidth(130);
        sLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#555;");

        Label pLbl = new Label(price);
        pLbl.setPrefWidth(110);
        pLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#E53238;");

        String badgeColors = switch (status) {
            case "RUNNING" -> "#EAF5EA:#5BA55B";
            case "OPEN" -> "#E8F0FE:#1A73E8";
            case "FINISHED" -> "#F4F4F4:#888888";
            default -> "#F4F4F4:#888888";
        };
        Label statusLbl = buildSmallBadge(status, badgeColors);
        statusLbl.setPrefWidth(90);

        Button viewBtn = new Button("View");
        viewBtn.setStyle(smallBtnStyle(false));
        viewBtn.setOnAction(e -> SceneNavigator.navigateTo(SceneNavigator.View.AUCTION_DETAIL));

        HBox actions = new HBox(6, viewBtn);
        actions.setPrefWidth(100);

        row.getChildren().addAll(tLbl, sLbl, pLbl, statusLbl, actions);
        return row;
    }

    // ── Helpers ──────────────────────────────────────────────

    /** colors format: "bg:fg" */
    private Label buildSmallBadge(String text, String colors) {
        String[] c = colors.split(":");
        Label l = new Label(text);
        l.setStyle("-fx-background-color:" + c[0] + ";-fx-text-fill:" + c[1] + ";" +
                "-fx-font-size:10px;-fx-font-weight:bold;" +
                "-fx-padding:3px 8px;-fx-background-radius:10px;");
        return l;
    }

    private String smallBtnStyle(boolean danger) {
        if (danger)
            return "-fx-background-color:#FDECEA;-fx-text-fill:#E53238;" +
                    "-fx-border-color:transparent;-fx-border-radius:4px;" +
                    "-fx-background-radius:4px;-fx-font-size:11px;" +
                    "-fx-padding:3px 10px;-fx-cursor:hand;-fx-effect:null;";
        return "-fx-background-color:white;-fx-text-fill:#555;" +
                "-fx-border-color:#E0E0E0;-fx-border-width:1px;" +
                "-fx-border-radius:4px;-fx-background-radius:4px;" +
                "-fx-font-size:11px;-fx-padding:3px 10px;-fx-cursor:hand;-fx-effect:null;";
    }
}