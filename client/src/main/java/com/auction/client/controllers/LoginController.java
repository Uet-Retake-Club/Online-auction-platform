package com.auction.client.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import com.auction.client.services.NetworkClientService;
import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.UserSession;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;

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
 *
 * THAY ĐỔI SO VỚI PHIÊN BẢN CŨ:
 *  - Thêm method onFocusPassword() → khi nhấn Enter ở emailField,
 *    focus tự chuyển xuống passwordField
 *  - PasswordField đã có onAction="#onLogin" trong FXML
 *    → nhấn Enter ở password = nhấn nút Sign in
 */
public class LoginController implements Initializable {

    @FXML private HBox          rootPane;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         emailError;
    @FXML private Label         passwordError;
    @FXML private Label         generalError;
    @FXML private Button        loginButton;
    @FXML private Label         signUpLabel;
    @FXML private Label         forgotPasswordLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Xóa lỗi khi người dùng bắt đầu gõ lại
        emailField.textProperty().addListener(
            (obs, old, val) -> clearFieldError(emailError, emailField));
        passwordField.textProperty().addListener(
            (obs, old, val) -> clearFieldError(passwordError, passwordField));
    }

    // ── MỚI: Enter ở emailField → focus xuống passwordField ──
    /**
     * Được gọi khi nhấn Enter trong ô Email.
     * Chuyển focus xuống ô Password thay vì submit ngay.
     * Đây là UX chuẩn của mọi login form.
     */
    @FXML
    private void onFocusPassword() {
        passwordField.requestFocus();
    }

    // ── Sign in (gọi từ nút Sign in VÀ từ Enter ở passwordField) ──
    @FXML
    private void onLogin() {
        if (!validateFields()) return;

        String email = emailField.getText().trim();
        // THIẾT LẬP SESSION (Giả lập user ID từ email)
        String username = email.split("@")[0];
        UserSession.getInstance().setUsername(username);

        // ĐẢM BẢO KẾT NỐI SOCKET ĐÃ SẴN SÀNG (retry nếu cần)
        NetworkClientService.getInstance().ensureConnected();

        // Khởi tạo BidService sớm để đăng ký listener trước khi nhận message
        com.auction.client.services.BidService.getInstance();

        // GỬI LỆNH LOGIN QUA SOCKET ĐỂ SERVER BIẾT AI ĐANG ONLINE
        // Chờ 500ms cho kết nối ổn định nếu vừa mới retry
        new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            Request loginReq = new Request(MessageType.LOGIN, username, "");
            NetworkClientService.getInstance().sendRequest(loginReq);
        }).start();

        SceneNavigator.navigateTo(SceneNavigator.View.HOME);
    }

    @FXML
    private void onGoToSignUp() {
        SceneNavigator.navigateTo(SceneNavigator.View.SIGNUP);
    }

    @FXML
    private void onForgotPassword() {
        showGeneralError("Chức năng này chưa được triển khai.");
    }

    // ── Validation ────────────────────────────────────────────

    private boolean validateFields() {
        boolean ok = true;

        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            showFieldError(emailError, emailField, "Email là bắt buộc");
            ok = false;
        } else if (!email.contains("@")) {
            showFieldError(emailError, emailField, "Email không hợp lệ");
            ok = false;
        }

        String password = passwordField.getText();
        if (password.isEmpty()) {
            showFieldError(passwordError, passwordField, "Mật khẩu là bắt buộc");
            ok = false;
        } else if (password.length() < 4) {
            showFieldError(passwordError, passwordField, "Mật khẩu quá ngắn");
            ok = false;
        }

        return ok;
    }

    // ── Helpers ───────────────────────────────────────────────

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
