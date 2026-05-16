package com.auction.client.controllers;

import com.auction.client.services.NetworkClientService;
import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.UserSession;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import com.auction.shared.dto.AdminStats;
import com.auction.shared.models.Item;
import com.auction.shared.models.User;
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
  @FXML
  private Button navWallet;
  @FXML
  private VBox walletTab;
  @FXML
  private VBox walletTableContainer;

  private final Gson gson = new Gson();

  private Button activeNav;


  @Override
  public void initialize(final URL url, final ResourceBundle rb) {
    adminLabel.setText(UserSession.getInstance().getDisplayName());
    activeNav = navDashboard;

    // Reset stats
    statTotalUsers.setText("...");
    statActiveAuctions.setText("...");
    statTotalBids.setText("...");
    statRevenue.setText("...");

    Platform.runLater(this::fetchStats);
  }

  private void fetchStats() {
    NetworkClientService.getInstance().addListener(this::handleAdminResponse);
    NetworkClientService.getInstance().sendRequest(new Request(MessageType.ADMIN_GET_STATS, UserSession.getInstance().getUserId(), ""));
  }

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
    dashboardTab.setVisible(false);
    dashboardTab.setManaged(false);
    walletTab.setVisible(false);
    walletTab.setManaged(false);
    tabPlaceholder.setVisible(true);
    tabPlaceholder.setManaged(true);
    ((Label) tabPlaceholder.getChildren().get(0)).setText("Loading users...");
    
    Platform.runLater(this::fetchUsers);
  }

  private void fetchUsers() {
    NetworkClientService.getInstance().addListener(this::handleAdminResponse);
    NetworkClientService.getInstance().sendRequest(new Request(MessageType.ADMIN_GET_USERS, UserSession.getInstance().getUserId(), ""));
  }

  @FXML
  private void onTabAuctions() {
    switchNav(navAuctions);
    dashboardTab.setVisible(false);
    dashboardTab.setManaged(false);
    walletTab.setVisible(false);
    walletTab.setManaged(false);
    tabPlaceholder.setVisible(true);
    tabPlaceholder.setManaged(true);
    ((Label) tabPlaceholder.getChildren().get(0)).setText("Loading auctions...");

    Platform.runLater(this::fetchAuctions);
  }

  private void fetchAuctions() {
    NetworkClientService.getInstance().addListener(this::handleAdminResponse);
    NetworkClientService.getInstance().sendRequest(new Request(MessageType.ADMIN_GET_AUCTIONS, UserSession.getInstance().getUserId(), ""));
  }

  @FXML
  private void onTabBids() {
    switchNav(navBids);
    showPlaceholder("Bids log - coming soon");
  }

  @FXML
  private void onTabWallet() {
    switchNav(navWallet);
    dashboardTab.setVisible(false);
    dashboardTab.setManaged(false);
    tabPlaceholder.setVisible(false);
    tabPlaceholder.setManaged(false);
    walletTab.setVisible(true);
    walletTab.setManaged(true);
    Platform.runLater(this::fetchPendingRequests);
  }

  private void fetchPendingRequests() {
    final Request req = new Request(MessageType.ADMIN_GET_PENDING_TOPUPS, UserSession.getInstance().getUserId(), "");
    NetworkClientService.getInstance().addListener(this::handleAdminResponse);
    NetworkClientService.getInstance().sendRequest(req);
  }

  private void handleAdminResponse(Response response) {
    if (response.getType() == MessageType.ADMIN_STATS_RESPONSE) {
      NetworkClientService.getInstance().removeListener(this::handleAdminResponse);
      Platform.runLater(() -> {
        try {
          final AdminStats stats = gson.fromJson(response.getPayload(), AdminStats.class);
          statTotalUsers.setText(String.format("%, d", stats.totalUsers));
          statActiveAuctions.setText(String.format("%, d", stats.activeAuctions));
          statTotalBids.setText(String.format("%, d", stats.totalBids));
          statRevenue.setText(String.format("$%,.2f", stats.revenue));
        } catch (Exception e) {
          LOGGER.log(Level.SEVERE, "Error parsing stats", e);
        }
      });
    } else if (response.getType() == MessageType.ADMIN_USERS_RESPONSE) {
      NetworkClientService.getInstance().removeListener(this::handleAdminResponse);
      Platform.runLater(() -> {
        try {
          final User[] users = gson.fromJson(response.getPayload(), User[].class);
          loadUsersTable(users);
          tabPlaceholder.setVisible(false);
          tabPlaceholder.setManaged(false);
          dashboardTab.setVisible(true);
          dashboardTab.setManaged(true);
          // Show users, hide stats area if needed, but for now just load into the container
          usersTableContainer.getParent().setVisible(true);
          auctionsTableContainer.getParent().setVisible(false);
        } catch (Exception e) {
          LOGGER.log(Level.SEVERE, "Error parsing users", e);
        }
      });
    } else if (response.getType() == MessageType.ADMIN_AUCTIONS_RESPONSE) {
      NetworkClientService.getInstance().removeListener(this::handleAdminResponse);
      Platform.runLater(() -> {
        try {
          final Item[] items = gson.fromJson(response.getPayload(), Item[].class);
          loadAuctionsTable(items);
          tabPlaceholder.setVisible(false);
          tabPlaceholder.setManaged(false);
          dashboardTab.setVisible(true);
          dashboardTab.setManaged(true);
          usersTableContainer.getParent().setVisible(false);
          auctionsTableContainer.getParent().setVisible(true);
        } catch (Exception e) {
          LOGGER.log(Level.SEVERE, "Error parsing auctions", e);
        }
      });
    } else if (response.getType() == MessageType.ADMIN_PENDING_TOPUPS_RESPONSE) {
      NetworkClientService.getInstance().removeListener(this::handleAdminResponse);
      Platform.runLater(() -> {
        try {
          final TopupRequest[] requests = gson.fromJson(response.getPayload(), TopupRequest[].class);
          loadWalletTable(requests);
        } catch (Exception e) {
          LOGGER.log(Level.SEVERE, "Error parsing pending topups", e);
        }
      });
    } else if (response.getType() == MessageType.ADMIN_APPROVE_TOPUP || response.getType() == MessageType.ADMIN_REJECT_TOPUP) {
      Platform.runLater(() -> {
        ToastNotification.show(rootPane, response.getMessage(), response.getStatus().equals("SUCCESS") ? ToastNotification.Type.SUCCESS : ToastNotification.Type.DANGER);
        fetchPendingRequests(); // Refresh list
      });
    }
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
    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    row.setStyle("-fx-border-color:transparent transparent #F4F4F4 transparent;"
        + "-fx-border-width:0 0 1px 0;-fx-padding:9px 16px;");

    final Label idLabel = new Label(tr.id);
    idLabel.setPrefWidth(120);
    idLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#888;");

    final Label userLabel = new Label(tr.userId);
    userLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#111;");
    HBox.setHgrow(userLabel, Priority.ALWAYS);

    final Label amountLabel = new Label(String.format("$%,.2f", tr.amount));
    amountLabel.setPrefWidth(120);
    amountLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#5BA55B;");

    final java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
    final Label timeLabel = new Label(sdf.format(new java.util.Date(tr.timestamp)));
    timeLabel.setPrefWidth(160);
    timeLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#555;");

    final Button approveBtn = new Button("Approve");
    approveBtn.setStyle("-fx-background-color:#EAF5EA;-fx-text-fill:#5BA55B;-fx-font-size:11px;-fx-padding:4px 12px;-fx-cursor:hand;");
    approveBtn.setOnAction(e -> approveRequest(tr.id));

    final Button rejectBtn = new Button("Reject");
    rejectBtn.setStyle("-fx-background-color:#FDECEA;-fx-text-fill:#E53238;-fx-font-size:11px;-fx-padding:4px 12px;-fx-cursor:hand;");
    rejectBtn.setOnAction(e -> rejectRequest(tr.id));

    final HBox actions = new HBox(8, approveBtn, rejectBtn);
    actions.setPrefWidth(180);

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

  private void showPlaceholder(final String msg) {
    dashboardTab.setVisible(false);
    dashboardTab.setManaged(false);
    walletTab.setVisible(false);
    walletTab.setManaged(false);
    tabPlaceholder.setVisible(true);
    tabPlaceholder.setManaged(true);
    ((Label) tabPlaceholder.getChildren().get(0)).setText(msg);
  }

  @FXML
  private void onLogout() {
    UserSession.getInstance().clear();
    SceneNavigator.navigateTo(SceneNavigator.View.LOGIN);
  }

  private void loadUsersTable(final User[] users) {
    usersTableContainer.getChildren().clear();
    if (users == null) return;
    for (User u : users) {
      usersTableContainer.getChildren().add(buildUserRow(u.getUsername(), u.getEmail(), u.getRole(), "Active"));
    }
  }

  private HBox buildUserRow(final String username, final String email,
      final String role, final String status) {
    final HBox row = new HBox();
    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    row.setStyle("-fx-border-color:transparent transparent #F4F4F4 transparent;"
        + "-fx-border-width:0 0 1px 0;-fx-padding:9px 16px;");

    final Label usernameLabel = new Label(username);
    usernameLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#111;");
    HBox.setHgrow(usernameLabel, Priority.ALWAYS);

    final Label emailLabel = new Label(email);
    emailLabel.setPrefWidth(200);
    emailLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#555;");

    final Label roleLabel = buildSmallBadge(role,
        role.equalsIgnoreCase("Admin") || role.equalsIgnoreCase("Seller") ? "#F4F4F4:#555555" : "#E8F0FE:#1A73E8");
    roleLabel.setPrefWidth(90);

    final Label statusLabel = buildSmallBadge(status,
        status.equals("Active") ? "#EAF5EA:#5BA55B" : "#FEF6E6:#F5A623");
    statusLabel.setPrefWidth(90);

    final Button editBtn = new Button("Edit");
    editBtn.setStyle(smallBtnStyle(false));
    editBtn.setOnAction(e -> System.out.println("TODO: edit user " + username));

    final boolean isBanned = status.equals("Suspended");
    final Button banBtn = new Button(isBanned ? "Unban" : "Ban");
    banBtn.setStyle(smallBtnStyle(true));
    banBtn.setOnAction(e -> System.out.println("TODO: toggle ban " + username));

    final HBox actions = new HBox(6, editBtn, banBtn);
    actions.setPrefWidth(120);

    row.getChildren().addAll(usernameLabel, emailLabel, roleLabel, statusLabel, actions);
    return row;
  }

  private void loadAuctionsTable(final Item[] auctions) {
    auctionsTableContainer.getChildren().clear();
    if (auctions == null) return;
    for (Item a : auctions) {
      loadAuctionsTableItem(a);
    }
  }

  private void loadAuctionsTableItem(final Item a) {
    auctionsTableContainer.getChildren().add(buildAuctionRow(a.getName(), a.getSellerId(), String.format("$%,.2f", a.getCurrentHighestBid()), a.getStatus()));
  }

  private HBox buildAuctionRow(final String title, final String seller,
      final String price, final String status) {
    final HBox row = new HBox();
    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    row.setStyle("-fx-border-color:transparent transparent #F4F4F4 transparent;"
        + "-fx-border-width:0 0 1px 0;-fx-padding:9px 16px;");

    final Label titleLabel = new Label(title);
    titleLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#111;");
    HBox.setHgrow(titleLabel, Priority.ALWAYS);

    final Label sellerLabel = new Label(seller);
    sellerLabel.setPrefWidth(130);
    sellerLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#555;");

    final Label priceLabel = new Label(price);
    priceLabel.setPrefWidth(110);
    priceLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#E53238;");

    final String badgeColors = switch (status) {
      case "RUNNING" -> "#EAF5EA:#5BA55B";
      case "OPEN" -> "#E8F0FE:#1A73E8";
      default -> "#F4F4F4:#888888";
    };
    final Label statusLabel = buildSmallBadge(status, badgeColors);
    statusLabel.setPrefWidth(90);

    final Button viewBtn = new Button("View");
    viewBtn.setStyle(smallBtnStyle(false));
    viewBtn.setOnAction(e -> SceneNavigator.navigateTo(SceneNavigator.View.AUCTION_DETAIL));

    final HBox actions = new HBox(6, viewBtn);
    actions.setPrefWidth(100);

    row.getChildren().addAll(titleLabel, sellerLabel, priceLabel, statusLabel, actions);
    return row;
  }

  /**
   * Builds a small colored badge.
   *
   * @param text the text
   * @param colors format bg:fg
   * @return a formatted Label
   */
  private Label buildSmallBadge(final String text, final String colors) {
    final String[] c = colors.split(":");
    final Label l = new Label(text);
    l.setStyle("-fx-background-color:" + c[0] + ";-fx-text-fill:" + c[1] + ";"
        + "-fx-font-size:10px;-fx-font-weight:bold;"
        + "-fx-padding:3px 8px;-fx-background-radius:10px;");
    return l;
  }

  private String smallBtnStyle(final boolean danger) {
    if (danger) {
      return "-fx-background-color:#FDECEA;-fx-text-fill:#E53238;"
          + "-fx-border-color:transparent;-fx-border-radius:4px;"
          + "-fx-background-radius:4px;-fx-font-size:11px;"
          + "-fx-padding:3px 10px;-fx-cursor:hand;-fx-effect:null;";
    }
    return "-fx-background-color:white;-fx-text-fill:#555;"
        + "-fx-border-color:#E0E0E0;-fx-border-width:1px;"
        + "-fx-border-radius:4px;-fx-background-radius:4px;"
        + "-fx-font-size:11px;-fx-padding:3px 10px;-fx-cursor:hand;-fx-effect:null;";
  }

  @FXML
  private void onToggleTheme() {
    SceneNavigator.toggleTheme();
  }

  @FXML
  private void onHome() {
    SceneNavigator.navigateTo(SceneNavigator.View.HOME);
  }
}