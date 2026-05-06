package com.auction.client.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import com.auction.client.services.AuthService;
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
 * SignupController.java
 * ─────────────────────────────────────────────
 * Handles SignUpView.fxml.
 */
public class SignupController implements Initializable {

    @FXML private HBox          rootPane;
    @FXML private TextField     firstNameField;
    @FXML private TextField     lastNameField;
    @FXML private TextField     usernameField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         firstNameError;
    @FXML private Label         lastNameError;
    @FXML private Label         usernameError;
    @FXML private Label         emailError;
    @FXML private Label         passwordError;
    @FXML private Label         confirmPasswordError;
    @FXML private Label         generalError;
    @FXML private Button        signUpButton;
    @FXML private Label         loginLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        firstNameField.textProperty().addListener(
            (o,old,v) -> clearFieldError(firstNameError, firstNameField));
        lastNameField.textProperty().addListener(
            (o,old,v) -> clearFieldError(lastNameError, lastNameField));
        usernameField.textProperty().addListener(
            (o,old,v) -> clearFieldError(usernameError, usernameField));
        emailField.textProperty().addListener(
            (o,old,v) -> clearFieldError(emailError, emailField));
        passwordField.textProperty().addListener(
            (o,old,v) -> clearFieldError(passwordError, passwordField));
        confirmPasswordField.textProperty().addListener(
            (o,old,v) -> clearFieldError(confirmPasswordError, confirmPasswordField));
    }

    // ── Handlers ─────────────────────────────────────────────

    @FXML
    private void onSignUp() {
        if (!validateFields()) return;

        AuthService authService = new AuthService();
        authService.register(
            firstNameField.getText(),
            lastNameField.getText(),
            usernameField.getText(),
            emailField.getText(),
            passwordField.getText()
        ).thenAccept(user -> {
            UserSession.getInstance().login(user);
            javafx.application.Platform.runLater(() -> 
                SceneNavigator.navigateTo(SceneNavigator.View.HOME)
            );
        }).exceptionally(err -> {
            javafx.application.Platform.runLater(() -> 
                showGeneralError(err.getMessage())
            );
            return null;
        });
    }

    @FXML
    private void onGoToLogin() {
        SceneNavigator.navigateTo(SceneNavigator.View.LOGIN);
    }

    // ── Validation ───────────────────────────────────────────

    private boolean validateFields() {
        boolean ok = true;

        if (firstNameField.getText().trim().isEmpty()) {
            showFieldError(firstNameError, firstNameField, "First name is required");
            ok = false;
        }
        if (lastNameField.getText().trim().isEmpty()) {
            showFieldError(lastNameError, lastNameField, "Last name is required");
            ok = false;
        }

        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            showFieldError(usernameError, usernameField, "Username is required");
            ok = false;
        } else if (username.length() < 3) {
            showFieldError(usernameError, usernameField, "At least 3 characters");
            ok = false;
        }

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
        } else if (password.length() < 8) {
            showFieldError(passwordError, passwordField, "At least 8 characters");
            ok = false;
        }

        String confirm = confirmPasswordField.getText();
        if (confirm.isEmpty()) {
            showFieldError(confirmPasswordError, confirmPasswordField, "Please confirm your password");
            ok = false;
        } else if (!confirm.equals(password)) {
            showFieldError(confirmPasswordError, confirmPasswordField, "Passwords do not match");
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