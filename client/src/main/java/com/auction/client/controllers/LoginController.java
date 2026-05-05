package com.auction.client.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.UserSession;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/**
 * LoginController.java
 * ─────────────────────────────────────────────
 * Handles LoginView.fxml.
 * Navigation is fully wired — no backend needed yet.
 *
 * Flow:
 *   "Sign in"    → validates fields → goes to HomeView
 *   "Create one" → goes to SignUpView
 */
public class LoginController implements Initializable {

    @FXML private HBox         rootPane;
    @FXML private TextField    emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label        emailError;
    @FXML private Label        passwordError;
    @FXML private Label        generalError;
    @FXML private Button       loginButton;
    @FXML private Label        signUpLabel;
    @FXML private Label        forgotPasswordLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Clear field errors as the user types
        emailField.textProperty().addListener(
            (obs, old, val) -> clearFieldError(emailError, emailField));
        passwordField.textProperty().addListener(
            (obs, old, val) -> clearFieldError(passwordError, passwordField));
    }

    // ── Button / link handlers ───────────────────────────────

    @FXML
    private void onLogin() {
        if (!validateFields()) return;

        String email = emailField.getText().trim();
        String username = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
        UserSession.getInstance().signIn("", "", username, email, "BIDDER");

        SceneNavigator.navigateTo(SceneNavigator.View.HOME);
    }

    @FXML
    private void onGoToSignUp() {
        SceneNavigator.navigateTo(SceneNavigator.View.SIGNUP);
    }

    @FXML
    private void onForgotPassword() {
        // TODO: open forgot-password screen when built
        showGeneralError("Forgot password is not available yet.");
    }

    // ── Validation ───────────────────────────────────────────

    private boolean validateFields() {
        boolean ok = true;

        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            showFieldError(emailError, emailField, "Email is required");
            ok = false;
        } else if (!email.contains("@")) {
            showFieldError(emailError, emailField, "Enter a valid email address");
            ok = false;
        }

        String password = passwordField.getText();
        if (password.isEmpty()) {
            showFieldError(passwordError, passwordField, "Password is required");
            ok = false;
        } else if (password.length() < 4) {
            showFieldError(passwordError, passwordField, "Password is too short");
            ok = false;
        }

        return ok;
    }

    // ── Helpers ──────────────────────────────────────────────

    private void showFieldError(Label label, Control field, String msg) {
        label.setText(msg);
        label.setVisible(true);
        field.getStyleClass().remove("error");
        field.getStyleClass().add("error");
        generalError.setText("");
    }

    private void clearFieldError(Label label, Control field) {
        label.setText("");
        label.setVisible(false);
        field.getStyleClass().remove("error");
    }

    private void showGeneralError(String msg) {
        generalError.setText(msg);
        generalError.setVisible(true);
    }
}