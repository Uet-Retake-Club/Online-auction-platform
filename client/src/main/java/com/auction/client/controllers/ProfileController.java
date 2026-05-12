package com.auction.client.controllers;

import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.UserSession;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * ProfileController handles user profile updates.
 */
public class ProfileController implements Initializable {

  @FXML private Label userLabel;
  @FXML private Label userTitle;
  @FXML private TextField firstNameField;
  @FXML private TextField lastNameField;
  @FXML private TextField usernameField;
  @FXML private TextField emailField;
  @FXML private TextField roleField;
  @FXML private Label statusLabel;
  @FXML private Button saveBtn;

  @Override
  public void initialize(final URL url, final ResourceBundle rb) {
    loadUserProfile();
  }

  private void loadUserProfile() {
    final UserSession session = UserSession.getInstance();
    userLabel.setText(session.getInitials());
    userTitle.setText("Hello, " + session.getDisplayName());
    firstNameField.setText(session.getFirstName());
    lastNameField.setText(session.getLastName());
    usernameField.setText(session.getUsername());
    emailField.setText(session.getEmail());
    roleField.setText(session.getRole());
    roleField.setDisable(true);
    statusLabel.setText("");
  }

  @FXML
  private void onSave() {
    final String firstName = firstNameField.getText().trim();
    final String lastName = lastNameField.getText().trim();
    final String username = usernameField.getText().trim();

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
    
    final String email = emailField.getText().trim();
    if (email.isEmpty() || !email.contains("@")) {
      showStatus("Enter a valid email address.", "#E53238");
      return;
    }

    UserSession.getInstance().signIn(firstName, lastName, username, email, 
        UserSession.getInstance().getRole());
    userLabel.setText(UserSession.getInstance().getInitials());
    userTitle.setText("Hello, " + UserSession.getInstance().getDisplayName());
    showStatus("Profile updated successfully.", "#5BA55B");
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

  private void showStatus(final String message, final String color) {
    statusLabel.setText(message);
    statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
  }
}