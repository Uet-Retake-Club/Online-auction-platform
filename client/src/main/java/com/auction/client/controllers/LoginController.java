package com.auction.client.controllers;

import com.auction.client.utils.SceneNavigator;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/**
 * LoginController — handles LoginView.fxml.
 *
 * <p>Pressing Enter in the email field moves focus to the password field.
 * Pressing Enter in the password field (or clicking Sign in) submits the form.
 */
public class LoginController implements Initializable {

  /** Root pane injected by FXML. */
  @FXML
  private HBox rootPane;

  /** Email input field. */
  @FXML
  private TextField emailField;

  /** Password input field. */
  @FXML
  private PasswordField passwordField;

  /** Inline error label for email. */
  @FXML
  private Label emailError;

  /** Inline error label for password. */
  @FXML
  private Label passwordError;

  /** General error label (e.g. wrong credentials). */
  @FXML
  private Label generalError;

  /** Sign-in button. */
  @FXML
  private Button loginButton;

  /** Link to sign-up screen. */
  @FXML
  private Label signUpLabel;

  /** Forgot-password link. */
  @FXML
  private Label forgotPasswordLabel;

  @Override
  public void initialize(final URL url, final ResourceBundle rb) {
    emailField.textProperty().addListener(
        (obs, old, val) -> clearFieldError(emailError, emailField));
    passwordField.textProperty().addListener(
        (obs, old, val) -> clearFieldError(passwordError, passwordField));
  }

  /**
   * Called when Enter is pressed in the email field.
   * Moves focus to the password field.
   */
  @FXML
  private void onFocusPassword() {
    passwordField.requestFocus();
  }

  /**
   * Called when the Sign in button is clicked or Enter is pressed
   * in the password field.
   */
  @FXML
  private void onLogin() {
    if (!validateFields()) {
      return;
    }
    
    // Mock login session since AuthService is not yet implemented
    final String email = emailField.getText().trim();
    final String username = email.contains("@")
        ? email.substring(0, email.indexOf("@")) : email;
    final com.auction.client.utils.UserSession session =
        com.auction.client.utils.UserSession.getInstance();
    session.signIn("Test", "User", username, email, "BIDDER");
    session.setWalletBalance(2450.00);
    
    // Connect the socket to the server so bidding works
    final com.auction.client.services.NetworkClientService network =
        com.auction.client.services.NetworkClientService.getInstance();
    network.connect("localhost", 8080);

    // Send LOGIN handshake after socket is established
    new Thread(() -> {
      final int maxWait = 20;
      for (int i = 0; i < maxWait; i++) {
        if (network.isConnected()) {
          break;
        }
        try {
          Thread.sleep(500);
        } catch (InterruptedException ignored) {
          Thread.currentThread().interrupt();
          return;
        }
      }

      if (network.isConnected()) {
        final com.auction.shared.dto.Request loginReq =
            new com.auction.shared.dto.Request(
                com.auction.shared.dto.MessageType.LOGIN,
                username, "");
        network.sendRequest(loginReq);
        System.out.println("[CLIENT] LOGIN request sent to server.");
        Platform.runLater(() -> SceneNavigator.navigateAfterLogin());
      } else {
        Platform.runLater(() -> showGeneralError(
            "Không thể kết nối tới Server. Vui lòng thử lại sau."));
        System.err.println("[CLIENT] Could not send LOGIN — not connected.");
      }
    }, "Client-Login-Thread").start();
  }

  /** Navigates to the sign-up screen. */
  @FXML
  private void onGoToSignUp() {
    SceneNavigator.navigateTo(SceneNavigator.View.SIGNUP);
  }

  /** Placeholder for forgot-password flow. */
  @FXML
  private void onForgotPassword() {
    showGeneralError("Chức năng này chưa được triển khai.");
  }

  // ── Validation ────────────────────────────────────────────

  /**
   * Validates the email and password fields.
   *
   * @return true if all inputs are valid
   */
  private boolean validateFields() {
    boolean ok = true;
    final String email = emailField.getText().trim();
    if (email.isEmpty()) {
      showFieldError(emailError, emailField, "Email là bắt buộc");
      ok = false;
    } else if (!email.contains("@")) {
      showFieldError(emailError, emailField, "Email không hợp lệ");
      ok = false;
    }
    final String password = passwordField.getText();
    if (password.isEmpty()) {
      showFieldError(passwordError, passwordField, "Mật khẩu là bắt buộc");
      ok = false;
    } else if (password.length() < 4) {
      showFieldError(passwordError, passwordField, "Mật khẩu quá ngắn");
      ok = false;
    }
    return ok;
  }

  /**
   * Displays an inline error on a field.
   *
   * @param label the error label to populate
   * @param field the control to mark with error styling
   * @param msg   the error message
   */
  private void showFieldError(
      final Label label, final Control field, final String msg) {
    label.setText(msg);
    label.setVisible(true);
    field.getStyleClass().remove("error");
    field.getStyleClass().add("error");
    generalError.setText("");
  }

  /**
   * Clears the inline error from a field.
   *
   * @param label the error label to clear
   * @param field the control to remove error styling from
   */
  private void clearFieldError(final Label label, final Control field) {
    label.setText("");
    label.setVisible(false);
    field.getStyleClass().remove("error");
  }

  /**
   * Displays a general error message above the submit button.
   *
   * @param msg the error message
   */
  private void showGeneralError(final String msg) {
    generalError.setText(msg);
    generalError.setVisible(true);
  }
  
  @FXML
  private void onHome() {
  }

  @FXML
  private void onToggleTheme() {
    SceneNavigator.toggleTheme();
  }
}