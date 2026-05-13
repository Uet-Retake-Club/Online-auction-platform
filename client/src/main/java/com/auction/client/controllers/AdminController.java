package com.auction.client.controllers;

import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.UserSession;
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
 * AdminController handles the admin dashboard view.
 *
 * <p>Features: Dashboard stats, Users table, Auctions table, and Tab navigation.
 */
public class AdminController implements Initializable {

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

  private static final String[][] DUMMY_USERS = {
    {"user_alpha", "alpha@mail.com", "Bidder", "Active"},
    {"watch_king99", "king@mail.com", "Seller", "Active"},
    {"collector_vn", "col@mail.com", "Bidder", "Suspended"},
    {"techbid2025", "tech@mail.com", "Bidder", "Active"},
    {"admin_root", "admin@hub.com", "Admin", "Active"},
  };

  private static final String[][] DUMMY_AUCTIONS = {
    {"Vintage Rolex Submariner", "watch_king99", "$1,240.00", "RUNNING"},
    {"iPhone 15 Pro Max", "techbid2025", "$780.00", "RUNNING"},
    {"Nike Air Jordan 1", "collector_vn", "$210.00", "FINISHED"},
    {"MacBook Air M2", "user_alpha", "$820.00", "OPEN"},
  };

  @Override
  public void initialize(final URL url, final ResourceBundle rb) {
    adminLabel.setText(UserSession.getInstance().getDisplayName());
    activeNav = navDashboard;

    statTotalUsers.setText("1,284");
    statActiveAuctions.setText("342");
    statTotalBids.setText("8,741");
    statRevenue.setText("$94,200");

    loadUsersTable(DUMMY_USERS);
    loadAuctionsTable(DUMMY_AUCTIONS);
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
    tabPlaceholder.setVisible(true);
    tabPlaceholder.setManaged(true);
    ((Label) tabPlaceholder.getChildren().get(0)).setText(msg);
  }

  @FXML
  private void onLogout() {
    UserSession.getInstance().clear();
    SceneNavigator.navigateTo(SceneNavigator.View.LOGIN);
  }

  private void loadUsersTable(final String[][] users) {
    usersTableContainer.getChildren().clear();
    for (String[] u : users) {
      usersTableContainer.getChildren().add(buildUserRow(u[0], u[1], u[2], u[3]));
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
        role.equals("Admin") || role.equals("Seller") ? "#F4F4F4:#555555" : "#E8F0FE:#1A73E8");
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

  private void loadAuctionsTable(final String[][] auctions) {
    auctionsTableContainer.getChildren().clear();
    for (String[] a : auctions) {
      auctionsTableContainer.getChildren().add(buildAuctionRow(a[0], a[1], a[2], a[3]));
    }
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
}