package com.auction.client.controllers;

import com.auction.client.services.NetworkClientService;
import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.ToastNotification;
import com.auction.client.utils.UserSession;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.auction.shared.dto.Response;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import com.auction.shared.models.TopupRequest;
import com.google.gson.Gson;
import java.util.Arrays;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * ProfileController handles user profile updates and wallet display.
 */
public class ProfileController implements Initializable {
  private static final Logger LOGGER = Logger.getLogger(ProfileController.class.getName());


  @FXML private Label userLabel;
  @FXML private Label userTitle;
  @FXML private TextField firstNameField;
  @FXML private TextField lastNameField;
  @FXML private TextField usernameField;
  @FXML private TextField emailField;
  @FXML private TextField roleField;
  @FXML private Label statusLabel;
  @FXML private Button saveBtn;

  // Wallet
  @FXML private Label walletBalanceLabel;
  @FXML private Label walletStatusLabel;
  @FXML private TextField topUpField;

  // History
  @FXML private TableView<TopupRequest> historyTable;
  @FXML private TableColumn<TopupRequest, String> colDate;
  @FXML private TableColumn<TopupRequest, Double> colAmount;
  @FXML private TableColumn<TopupRequest, String> colStatus;

  @Override
  public void initialize(final URL url, final ResourceBundle rb) {
    setupHistoryTable();
    loadUserProfile();
    fetchWalletBalance();
    fetchWalletHistory();
  }

  private void setupHistoryTable() {
    colDate.setCellValueFactory(cellData -> {
      long ts = cellData.getValue().timestamp;
      return new javafx.beans.property.SimpleStringProperty(
          new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(ts)));
    });
    colAmount.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().amount));
    colStatus.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().status));
    
    // Custom styling for amount and status if needed
    colAmount.setCellFactory(tc -> new javafx.scene.control.TableCell<>() {
      @Override protected void updateItem(Double amount, boolean empty) {
        super.updateItem(amount, empty);
        if (empty || amount == null) setText(null);
        else setText(String.format("$%.2f", amount));
      }
    });
  }

  // ── Profile ──────────────────────────────────────────────────────────────

  private void loadUserProfile() {
    final UserSession session = UserSession.getInstance();
    userLabel.setText(session.getInitials());
    userTitle.setText("Hello, " + session.getDisplayName());
    firstNameField.setText(session.getFirstName());
    lastNameField.setText(session.getLastName());
    usernameField.setText(session.getUsername());
    emailField.setText(session.getEmail());
    roleField.setText(session.getRole());
    statusLabel.setText("");
    walletStatusLabel.setText("");
  }

  @FXML
  private void onSave() {
    final String firstName = firstNameField.getText().trim();
    final String lastName  = lastNameField.getText().trim();
    final String username  = usernameField.getText().trim();
    final String email     = emailField.getText().trim();

    if (firstName.isEmpty()) {
      showStatus("First name is required.", "#E53238");
      return;
    }
    if (lastName.isEmpty()) {
      showStatus("Last name is required.", "#E53238");
      return;
    }
    if (username.isEmpty()) {
      showStatus("Username is required.", "#E53238");
      return;
    }
    if (email.isEmpty() || !email.contains("@")) {
      showStatus("Enter a valid email address.", "#E53238");
      return;
    }

    UserSession.getInstance().signIn(
        UserSession.getInstance().getUserId(),
        firstName, lastName, username, email,
        UserSession.getInstance().getRole());

    userLabel.setText(UserSession.getInstance().getInitials());
    userTitle.setText("Hello, " + UserSession.getInstance().getDisplayName());
    showStatus("Profile updated successfully.", "#2E7D32");
    ToastNotification.show(userLabel, "Profile saved!", ToastNotification.Type.SUCCESS);
  }

  @FXML
  private void onBack() {
    SceneNavigator.navigateTo(SceneNavigator.View.HOME);
  }

  @FXML
  private void onLogout() {
    UserSession.getInstance().clear();
    SceneNavigator.navigateTo(SceneNavigator.View.LOGIN);
  }

  @FXML
  private void onHome() {
    SceneNavigator.navigateTo(SceneNavigator.View.HOME);
  }

  @FXML
  private void onProfile() {
    // already here
  }

  @FXML
  private void onToggleTheme() {
    SceneNavigator.toggleTheme();
  }

  private void showStatus(final String message, final String color) {
    statusLabel.setText(message);
    statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
  }

  // ── Wallet ───────────────────────────────────────────────────────────────

  /** Sends GET_WALLET_BALANCE and updates the balance label when response arrives. */
  private void fetchWalletBalance() {
    walletBalanceLabel.setText("Loading…");
    walletStatusLabel.setText("");

    final NetworkClientService.ServerMessageListener[] ref =
        new NetworkClientService.ServerMessageListener[1];

    ref[0] = (Response response) -> {
      if (response.getType() == MessageType.WALLET_BALANCE_RESPONSE) {
        NetworkClientService.getInstance().removeListener(ref[0]);
        Platform.runLater(() -> {
          try {
            final double balance = Double.parseDouble(response.getPayload());
            walletBalanceLabel.setText(String.format("$%,.2f", balance));
          } catch (NumberFormatException e) {
            walletBalanceLabel.setText("$—");
          }
        });
      }
    };

    NetworkClientService.getInstance().addListener(ref[0]);
    NetworkClientService.getInstance().sendRequest(
        new Request(MessageType.GET_WALLET_BALANCE,
            UserSession.getInstance().getUserId(), ""));
  }

  /** Handles "Refresh" button on the wallet card. */
  @FXML
  private void onRefreshWallet() {
    fetchWalletBalance();
    fetchWalletHistory();
    walletStatusLabel.setText("Refreshing…");
    ToastNotification.show(userLabel, "Wallet refreshed", ToastNotification.Type.INFO);
  }

  private void fetchWalletHistory() {
    final Request req = new Request(MessageType.GET_WALLET_HISTORY, UserSession.getInstance().getUserId(), "");
    final NetworkClientService.ServerMessageListener[] ref = new NetworkClientService.ServerMessageListener[1];
    ref[0] = response -> {
      if (response.getType() == MessageType.WALLET_HISTORY_RESPONSE) {
        NetworkClientService.getInstance().removeListener(ref[0]);
        Platform.runLater(() -> {
          try {
            TopupRequest[] history = new Gson().fromJson(response.getPayload(), TopupRequest[].class);
            historyTable.getItems().setAll(Arrays.asList(history));
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing wallet history", e);
          }
        });
      }
    };
    NetworkClientService.getInstance().addListener(ref[0]);
    NetworkClientService.getInstance().sendRequest(req);
  }

  /** Handles "Request Top-up" button. */
  @FXML
  private void onTopUp() {
    final String raw = topUpField.getText().trim();
    if (raw.isEmpty()) {
      walletStatusLabel.setText("Please enter an amount.");
      walletStatusLabel.setStyle(
          "-fx-text-fill: #FFCDD2; -fx-font-size: 12px; -fx-padding: 0 24px 14px 24px;");
      return;
    }

    double amount;
    try {
      amount = Double.parseDouble(raw);
    } catch (NumberFormatException e) {
      walletStatusLabel.setText("Invalid amount — numbers only.");
      walletStatusLabel.setStyle(
          "-fx-text-fill: #FFCDD2; -fx-font-size: 12px; -fx-padding: 0 24px 14px 24px;");
      return;
    }

    if (amount <= 0) {
      walletStatusLabel.setText("Amount must be greater than zero.");
      walletStatusLabel.setStyle(
          "-fx-text-fill: #FFCDD2; -fx-font-size: 12px; -fx-padding: 0 24px 14px 24px;");
      return;
    }

    // Send request
    final NetworkClientService.ServerMessageListener[] ref =
        new NetworkClientService.ServerMessageListener[1];

    ref[0] = (Response response) -> {
      if (response.getType() == MessageType.WALLET_TOPUP_APPROVE) {
        NetworkClientService.getInstance().removeListener(ref[0]);
        Platform.runLater(() -> {
          if ("SUCCESS".equals(response.getStatus())) {
            walletStatusLabel.setText("✓ Top-up request submitted. Pending admin approval.");
            walletStatusLabel.setStyle(
                "-fx-text-fill: #A5D6A7; -fx-font-size: 12px; -fx-padding: 0 24px 14px 24px;");
            topUpField.clear();
            ToastNotification.show(userLabel,
                "Top-up request sent!", ToastNotification.Type.SUCCESS);
          } else {
            walletStatusLabel.setText("✕ Failed to submit request. Try again.");
            walletStatusLabel.setStyle(
                "-fx-text-fill: #FFCDD2; -fx-font-size: 12px; -fx-padding: 0 24px 14px 24px;");
            ToastNotification.show(userLabel,
                "Top-up request failed.", ToastNotification.Type.DANGER);
          }
        });
      }
    };

    NetworkClientService.getInstance().addListener(ref[0]);
    NetworkClientService.getInstance().sendRequest(
        new Request(MessageType.WALLET_TOPUP_REQUEST,
            UserSession.getInstance().getUserId(),
            String.valueOf(amount)));
  }

  @FXML
  private void onWallet() {
    SceneNavigator.navigateTo(SceneNavigator.View.WALLET);
  }
}