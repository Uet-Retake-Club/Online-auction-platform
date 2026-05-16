package com.auction.client.controllers;

import com.auction.client.services.NetworkClientService;
import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.UserSession;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.shared.dto.AdminStats;
import com.auction.shared.models.BidTransaction;
import com.auction.shared.models.TopupRequest;
import com.auction.client.utils.ToastNotification;
import com.google.gson.Gson;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import java.util.logging.Logger;
import java.util.logging.Level;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * AdminController handles the admin dashboard view.
 *
 * <p>Features: Dashboard stats, Users table, Auctions table, and Tab navigation.
 */
public class AdminController implements Initializable {
  private static final Logger LOGGER = Logger.getLogger(AdminController.class.getName());


  @FXML
  private BorderPane rootPane;
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
  private VBox dashboardTab;
  @FXML
  private VBox usersTab;
  @FXML
  private VBox auctionsTab;
  @FXML
  private VBox walletTab;
  @FXML
  private VBox bidsTab;
  @FXML
  private VBox tabPlaceholder;
  @FXML
  private VBox usersTableContainer;
  @FXML
  private VBox auctionsTableContainer;
  @FXML
  private VBox walletTableContainer;
  @FXML
  private VBox bidsTableContainer;
  
  @FXML
  private Button navDashboard;
  @FXML
  private Button navUsers;
  @FXML
  private Button navAuctions;
  @FXML
  private Button navBids;
  @FXML
  private Button navWallet;

  private final Gson gson = new Gson();
  private Button activeNav;

  // Stubs for deserialization (avoids issues with abstract shared classes)
  private static class UserStub {
    String id, username, email, role, status;
  }
  private static class ItemStub {
    String id, name, sellerId, status, description;
    String category; // Use String for safety
    double currentHighestBid;
  }

  @Override
  public void initialize(final URL url, final ResourceBundle rb) {
    adminLabel.setText(UserSession.getInstance().getDisplayName());
    activeNav = navDashboard;

    // Reset stats
    statTotalUsers.setText("...");
    statActiveAuctions.setText("...");
    statTotalBids.setText("...");
    statRevenue.setText("...");

    // Register persistent listener
    NetworkClientService.getInstance().addListener(this::handleAdminResponse);

    Platform.runLater(this::fetchStats);
  }

  private void fetchStats() {
    NetworkClientService.getInstance().sendRequest(new Request(MessageType.ADMIN_GET_STATS, UserSession.getInstance().getUserId(), ""));
  }

  private void hideAllTabs() {
    dashboardTab.setVisible(false);
    dashboardTab.setManaged(false);
    usersTab.setVisible(false);
    usersTab.setManaged(false);
    auctionsTab.setVisible(false);
    auctionsTab.setManaged(false);
    walletTab.setVisible(false);
    walletTab.setManaged(false);
    bidsTab.setVisible(false);
    bidsTab.setManaged(false);
    tabPlaceholder.setVisible(false);
    tabPlaceholder.setManaged(false);
  }

  private void showPlaceholder(String message) {
    hideAllTabs();
    tabPlaceholder.setVisible(true);
    tabPlaceholder.setManaged(true);
    if (!tabPlaceholder.getChildren().isEmpty() && tabPlaceholder.getChildren().get(1) instanceof Label) {
      ((Label) tabPlaceholder.getChildren().get(1)).setText(message);
    }
  }

  @FXML
  private void onTabDashboard() {
    switchNav(navDashboard);
    hideAllTabs();
    dashboardTab.setVisible(true);
    dashboardTab.setManaged(true);
    fetchStats();
  }

  @FXML
  private void onTabUsers() {
    switchNav(navUsers);
    showPlaceholder("Loading users...");
    fetchUsers();
  }

  private void fetchUsers() {
    NetworkClientService.getInstance().sendRequest(new Request(MessageType.ADMIN_GET_USERS, UserSession.getInstance().getUserId(), ""));
  }

  @FXML
  private void onTabAuctions() {
    switchNav(navAuctions);
    showPlaceholder("Loading auctions...");
    fetchAuctions();
  }

  private void fetchAuctions() {
    NetworkClientService.getInstance().sendRequest(new Request(MessageType.ADMIN_GET_AUCTIONS, UserSession.getInstance().getUserId(), ""));
  }

  @FXML
  private void onTabBids() {
    switchNav(navBids);
    showPlaceholder("Loading bids...");
    fetchBids();
  }

  private void fetchBids() {
    NetworkClientService.getInstance().sendRequest(new Request(MessageType.ADMIN_GET_BIDS, UserSession.getInstance().getUserId(), ""));
  }

  @FXML
  private void onTabWallet() {
    switchNav(navWallet);
    showPlaceholder("Loading pending requests...");
    fetchPendingRequests();
  }

  private void fetchPendingRequests() {
    NetworkClientService.getInstance().sendRequest(new Request(MessageType.ADMIN_GET_PENDING_TOPUPS, UserSession.getInstance().getUserId(), ""));
  }

  private void handleAdminResponse(Response response) {
    Platform.runLater(() -> {
      try {
        if ("FAIL".equals(response.getStatus())) {
          ToastNotification.show(rootPane, response.getMessage(), ToastNotification.Type.DANGER);
          hideAllTabs();
          dashboardTab.setVisible(true);
          dashboardTab.setManaged(true);
          return;
        }

        switch (response.getType()) {
          case ADMIN_STATS_RESPONSE, ADMIN_GET_STATS -> {
            final AdminStats stats = gson.fromJson(response.getPayload(), AdminStats.class);
            if (stats != null) {
              statTotalUsers.setText(String.format("%,d", stats.totalUsers));
              statActiveAuctions.setText(String.format("%,d", stats.activeAuctions));
              statTotalBids.setText(String.format("%,d", stats.totalBids));
              statRevenue.setText(String.format("$%,.2f", stats.revenue));
            }
          }
          case ADMIN_USERS_RESPONSE, ADMIN_GET_USERS -> {
            final UserStub[] users = gson.fromJson(response.getPayload(), UserStub[].class);
            loadUsersTable(users);
            hideAllTabs();
            usersTab.setVisible(true);
            usersTab.setManaged(true);
          }
          case ADMIN_AUCTIONS_RESPONSE, ADMIN_GET_AUCTIONS -> {
            final ItemStub[] items = gson.fromJson(response.getPayload(), ItemStub[].class);
            loadAuctionsTable(items);
            hideAllTabs();
            auctionsTab.setVisible(true);
            auctionsTab.setManaged(true);
          }
          case ADMIN_BIDS_RESPONSE, ADMIN_GET_BIDS -> {
            final BidTransaction[] bids = gson.fromJson(response.getPayload(), BidTransaction[].class);
            loadBidsTable(bids);
            hideAllTabs();
            bidsTab.setVisible(true);
            bidsTab.setManaged(true);
          }
          case ADMIN_PENDING_TOPUPS_RESPONSE, ADMIN_GET_PENDING_TOPUPS -> {
            final TopupRequest[] requests = gson.fromJson(response.getPayload(), TopupRequest[].class);
            loadWalletTable(requests);
            hideAllTabs();
            walletTab.setVisible(true);
            walletTab.setManaged(true);
          }
          case ADMIN_APPROVE_TOPUP, ADMIN_REJECT_TOPUP, ADMIN_BAN_USER, ADMIN_UNBAN_USER -> {
            ToastNotification.show(rootPane, response.getMessage(), 
                "SUCCESS".equals(response.getStatus()) ? ToastNotification.Type.SUCCESS : ToastNotification.Type.DANGER);
            if (response.getType() == MessageType.ADMIN_APPROVE_TOPUP || response.getType() == MessageType.ADMIN_REJECT_TOPUP) {
              fetchPendingRequests();
            } else {
              fetchUsers();
            }
          }
          default -> { /* Ignore other messages */ }
        }
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Error processing admin response: " + response.getType(), e);
      }
    });
  }

  private void loadBidsTable(final BidTransaction[] bids) {
    bidsTableContainer.getChildren().clear();
    if (bids == null || bids.length == 0) {
      final Label lbl = new Label("No bids found");
      lbl.setStyle("-fx-text-fill:#888;-fx-padding:20px;");
      bidsTableContainer.getChildren().add(lbl);
      return;
    }
    for (BidTransaction b : bids) {
      bidsTableContainer.getChildren().add(buildBidRow(b));
    }
  }

  private HBox buildBidRow(final BidTransaction b) {
    final HBox row = new HBox();
    row.getStyleClass().add("table-row");
    row.setAlignment(Pos.CENTER_LEFT);

    final Label idLabel = new Label(b.getId());
    idLabel.setPrefWidth(160);
    idLabel.getStyleClass().add("body-small");

    final Label itemLabel = new Label(b.getItemId());
    itemLabel.setPrefWidth(120);
    itemLabel.getStyleClass().add("body-small");

    final Label bidderLabel = new Label(b.getBidderId());
    bidderLabel.getStyleClass().add("label-bold");
    HBox.setHgrow(bidderLabel, Priority.ALWAYS);
    bidderLabel.setMaxWidth(Double.MAX_VALUE);

    final Label amountLabel = new Label(String.format("$%,.2f", b.getBidAmount()));
    amountLabel.setPrefWidth(110);
    amountLabel.getStyleClass().addAll("label-bold", "price-tag");
    amountLabel.setStyle("-fx-font-size: 13px;");

    final java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    final Label timeLabel = new Label(sdf.format(new java.util.Date(b.getTimestamp())));
    timeLabel.setPrefWidth(160);
    timeLabel.getStyleClass().add("body-small");

    row.getChildren().addAll(idLabel, itemLabel, bidderLabel, amountLabel, timeLabel);
    return row;
  }

  private void loadWalletTable(final TopupRequest[] requests) {
    walletTableContainer.getChildren().clear();
    if (requests == null || requests.length == 0) {
      final Label lbl = new Label("No pending requests");
      lbl.setStyle("-fx-text-fill:#888;-fx-padding:20px;");
      walletTableContainer.getChildren().add(lbl);
      return;
    }
    for (TopupRequest tr : requests) {
      walletTableContainer.getChildren().add(buildWalletRequestRow(tr));
    }
  }

  private HBox buildWalletRequestRow(final TopupRequest tr) {
    final HBox row = new HBox();
    row.getStyleClass().add("table-row");
    row.setAlignment(Pos.CENTER_LEFT);

    final Label idLabel = new Label(tr.id);
    idLabel.setPrefWidth(120);
    idLabel.getStyleClass().add("body-small");

    final Label userLabel = new Label(tr.userId);
    userLabel.getStyleClass().add("label-bold");
    HBox.setHgrow(userLabel, Priority.ALWAYS);
    userLabel.setMaxWidth(Double.MAX_VALUE);

    final Label amountLabel = new Label(String.format("$%,.2f", tr.amount));
    amountLabel.setPrefWidth(120);
    amountLabel.getStyleClass().addAll("label-bold", "price-tag");
    amountLabel.setStyle("-fx-text-fill: -success; -fx-font-size: 13px;");

    final java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
    final Label timeLabel = new Label(sdf.format(new java.util.Date(tr.timestamp)));
    timeLabel.setPrefWidth(160);
    timeLabel.getStyleClass().add("body-small");

    final Button approveBtn = new Button("Approve");
    approveBtn.getStyleClass().addAll("button", "btn-primary");
    approveBtn.setStyle("-fx-background-color: -success; -fx-font-size: 11px; -fx-padding: 4px 12px;");
    approveBtn.setOnAction(e -> approveRequest(tr.id));

    final Button rejectBtn = new Button("Reject");
    rejectBtn.getStyleClass().addAll("button", "btn-outline");
    rejectBtn.setStyle("-fx-text-fill: -danger; -fx-border-color: -danger; -fx-font-size: 11px; -fx-padding: 4px 12px;");
    rejectBtn.setOnAction(e -> rejectRequest(tr.id));

    final HBox actions = new HBox(8, approveBtn, rejectBtn);
    actions.setPrefWidth(180);
    actions.setAlignment(Pos.CENTER_LEFT);

    row.getChildren().addAll(idLabel, userLabel, amountLabel, timeLabel, actions);
    return row;
  }

  private void approveRequest(String requestId) {
    NetworkClientService.getInstance().sendRequest(new Request(MessageType.ADMIN_APPROVE_TOPUP, UserSession.getInstance().getUserId(), requestId));
  }

  private void rejectRequest(String requestId) {
    NetworkClientService.getInstance().sendRequest(new Request(MessageType.ADMIN_REJECT_TOPUP, UserSession.getInstance().getUserId(), requestId));
  }

  private void switchNav(final Button btn) {
    if (activeNav != null) {
      activeNav.getStyleClass().remove("nav-item-active");
      activeNav.getStyleClass().add("nav-item");
    }
    btn.getStyleClass().remove("nav-item");
    btn.getStyleClass().add("nav-item-active");
    activeNav = btn;
  }

  @FXML
  private void onLogout() {
    NetworkClientService.getInstance().removeListener(this::handleAdminResponse);
    UserSession.getInstance().clear();
    SceneNavigator.navigateTo(SceneNavigator.View.LOGIN);
  }

  private void loadUsersTable(final UserStub[] users) {
    usersTableContainer.getChildren().clear();
    if (users == null) return;
    for (UserStub u : users) {
      usersTableContainer.getChildren().add(buildUserRow(u.id, u.username, u.email, u.role, u.status));
    }
  }

  private HBox buildUserRow(final String userId, final String username, final String email,
      final String role, final String status) {
    final HBox row = new HBox();
    row.getStyleClass().add("table-row");
    row.setAlignment(Pos.CENTER_LEFT);

    final Label usernameLabel = new Label(username);
    usernameLabel.getStyleClass().add("label-bold");
    HBox.setHgrow(usernameLabel, Priority.ALWAYS);
    usernameLabel.setMaxWidth(Double.MAX_VALUE);

    final Label emailLabel = new Label(email);
    emailLabel.setPrefWidth(200);
    emailLabel.getStyleClass().add("body-text");

    final Label roleLabel = buildSmallBadge(role, role.equalsIgnoreCase("Admin") ? "badge-danger" : "badge-info");
    roleLabel.setPrefWidth(90);

    final String statusClass = switch (status) {
      case "ACTIVE" -> "badge-success";
      case "SUSPENDED" -> "badge-danger";
      default -> "badge-warning";
    };
    final Label statusLabel = buildSmallBadge(status, statusClass);
    statusLabel.setPrefWidth(90);

    final Button editBtn = new Button("Edit");
    editBtn.getStyleClass().add("btn-outline");
    editBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3px 10px;");
    editBtn.setOnAction(e -> ToastNotification.show(rootPane, "Edit feature coming soon", ToastNotification.Type.INFO));

    final boolean isBanned = "SUSPENDED".equals(status);
    final Button banBtn = new Button(isBanned ? "Unban" : "Ban");
    banBtn.getStyleClass().add("btn-outline");
    banBtn.setStyle(String.format("-fx-text-fill: %s; -fx-border-color: %s; -fx-font-size: 11px; -fx-padding: 3px 10px;", 
                                   isBanned ? "-success" : "-danger", isBanned ? "-success" : "-danger"));
    banBtn.setOnAction(e -> {
      if (isBanned) {
        NetworkClientService.getInstance().sendRequest(new Request(MessageType.ADMIN_UNBAN_USER, UserSession.getInstance().getUserId(), userId));
      } else {
        NetworkClientService.getInstance().sendRequest(new Request(MessageType.ADMIN_BAN_USER, UserSession.getInstance().getUserId(), userId));
      }
    });

    final HBox actions = new HBox(6, editBtn, banBtn);
    actions.setPrefWidth(120);
    actions.setAlignment(Pos.CENTER_LEFT);

    row.getChildren().addAll(usernameLabel, emailLabel, roleLabel, statusLabel, actions);
    return row;
  }

  private void loadAuctionsTable(final ItemStub[] auctions) {
    auctionsTableContainer.getChildren().clear();
    if (auctions == null) return;
    for (ItemStub a : auctions) {
      auctionsTableContainer.getChildren().add(buildAuctionRow(a));
    }
  }

  private HBox buildAuctionRow(final ItemStub item) {
    final String title = item.name;
    final String seller = item.sellerId;
    final String price = String.format("$%,.2f", item.currentHighestBid);
    final String status = item.status;

    final HBox row = new HBox();
    row.getStyleClass().add("table-row");
    row.setAlignment(Pos.CENTER_LEFT);

    final Label titleLabel = new Label(title);
    titleLabel.getStyleClass().add("label-bold");
    HBox.setHgrow(titleLabel, Priority.ALWAYS);
    titleLabel.setMaxWidth(Double.MAX_VALUE);

    final Label sellerLabel = new Label(seller);
    sellerLabel.setPrefWidth(130);
    sellerLabel.getStyleClass().add("body-text");

    final Label priceLabel = new Label(price);
    priceLabel.setPrefWidth(110);
    priceLabel.getStyleClass().addAll("label-bold", "price-tag");
    priceLabel.setStyle("-fx-font-size: 13px;");

    final String badgeClass = switch (status) {
      case "RUNNING", "OPEN" -> "badge-info";
      case "FINISHED" -> "badge-success";
      default -> "badge-warning";
    };
    final Label statusLabel = buildSmallBadge(status, badgeClass);
    statusLabel.setPrefWidth(90);

    final Button viewBtn = new Button("View");
    viewBtn.getStyleClass().add("btn-outline");
    viewBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3px 10px;");
    viewBtn.setOnAction(e -> {
      UserSession.getInstance().setSelectedItemId(item.id);
      UserSession.getInstance().setSelectedAuctionTitle(item.name);
      UserSession.getInstance().setSelectedAuctionCategory(item.category != null ? item.category : "");
      UserSession.getInstance().setSelectedItemDescription(item.description);
      SceneNavigator.navigateTo(SceneNavigator.View.AUCTION_DETAIL);
    });

    final HBox actions = new HBox(6, viewBtn);
    actions.setPrefWidth(100);
    actions.setAlignment(Pos.CENTER_LEFT);

    row.getChildren().addAll(titleLabel, sellerLabel, priceLabel, statusLabel, actions);
    return row;
  }

  private Label buildSmallBadge(final String text, final String styleClass) {
    final Label l = new Label(text);
    l.getStyleClass().addAll("badge", styleClass);
    return l;
  }

  @FXML
  private void onToggleTheme() {
    SceneNavigator.toggleTheme();
  }

  @FXML
  private void onHome() {
    NetworkClientService.getInstance().removeListener(this::handleAdminResponse);
    SceneNavigator.navigateTo(SceneNavigator.View.HOME);
  }
}