package com.auction.client.controllers;

import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.TopNavUtils;
import com.auction.client.utils.UserSession;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * CreateListingController.java ───────────────────────────────────────── Xử lý
 * màn hình đăng sản phẩm đấu giá (CreateListingView.fxml).
 *
 * <p>Chức năng: - Validate tất cả trường: tiêu đề, danh mục, mô tả, giá, thời gian
 * - Publish listing → gửi lên server (TODO: AuctionService) - Save draft → lưu
 * tạm (TODO) - Chọn ảnh sản phẩm từ filesystem
 */
public class CreateListingController implements Initializable {

  @FXML private Label userLabel;
  @FXML private Button walletBalanceBtn;
  @FXML private TextField titleField;
  @FXML private ComboBox<String> categoryCombo;
  @FXML private TextArea descriptionField;
  @FXML private TextField startPriceField;
  @FXML private TextField incrementField;
  @FXML private javafx.scene.control.DatePicker startDatePicker;
  @FXML private ComboBox<String> startHourCombo;
  @FXML private ComboBox<String> startMinuteCombo;
  @FXML private javafx.scene.control.DatePicker endDatePicker;
  @FXML private ComboBox<String> endHourCombo;
  @FXML private ComboBox<String> endMinuteCombo;
  @FXML private VBox imageDropZone;
  @FXML private Label imageLabel;
  @FXML private Label titleError;
  @FXML private Label categoryError;
  @FXML private Label descriptionError;
  @FXML private Label startPriceError;
  @FXML private Label incrementError;
  @FXML private Label startTimeError;
  @FXML private Label endTimeError;
  @FXML private Label generalError;
  @FXML private Button publishBtn;
  @FXML private Button draftBtn;

  @Override
  public void initialize(final URL url, final ResourceBundle rb) {
    userLabel.setText(UserSession.getInstance().getInitials());
    TopNavUtils.updateWalletBalance(walletBalanceBtn);

    categoryCombo.getItems().addAll(
        "Electronics", "Fashion", "Home & Garden",
        "Sports", "Collectibles", "Vehicles", "Art", "Other");

    titleField.textProperty().addListener((o, v, n) -> clearError(titleError, titleField));
    descriptionField.textProperty().addListener(
        (o, v, n) -> clearError(descriptionError, descriptionField));
    startPriceField.textProperty().addListener(
        (o, v, n) -> clearError(startPriceError, startPriceField));
    incrementField.textProperty().addListener(
        (o, v, n) -> clearError(incrementError, incrementField));
    startDatePicker.valueProperty().addListener((o, v, n) -> clearDateError(startTimeError));
    endDatePicker.valueProperty().addListener((o, v, n) -> clearDateError(endTimeError));
    startHourCombo.getItems().addAll(buildHourOptions());
    startMinuteCombo.getItems().addAll(buildMinuteOptions());
    endHourCombo.getItems().addAll(buildHourOptions());
    endMinuteCombo.getItems().addAll(buildMinuteOptions());
  }

  @FXML
  private void onPublish() {
    if (!validateAll()) {
      return;
    }

    final String startTimestamp = getDateTimeString(
        startDatePicker, startHourCombo, startMinuteCombo);
    final String endTimestamp = getDateTimeString(
        endDatePicker, endHourCombo, endMinuteCombo);

    System.out.println("Publish: " + titleField.getText()
        + " | " + startTimestamp + " -> " + endTimestamp);
    SceneNavigator.navigateTo(SceneNavigator.View.HOME);
  }

  @FXML
  private void onSaveDraft() {
    System.out.println("Draft saved: " + titleField.getText());
    generalError.setText("Draft saved successfully.");
    generalError.setStyle("-fx-text-fill:#5BA55B;-fx-font-size:12px;");
  }

  @FXML
  private void onSelectImage() {
    imageLabel.setText("image_selected.jpg");
  }

  @FXML
  private void onCancel() {
    SceneNavigator.navigateTo(SceneNavigator.View.SELLER);
  }

  @FXML
  private void onHome() {
    SceneNavigator.navigateTo(SceneNavigator.View.HOME);
  }

  @FXML
  private void onMyListings() {
    SceneNavigator.navigateTo(SceneNavigator.View.SELLER);
  }

  @FXML
  private void onActiveAuctions() {
    SceneNavigator.navigateTo(SceneNavigator.View.SELLER);
  }

  @FXML
  private void onDrafts() {
    SceneNavigator.navigateTo(SceneNavigator.View.SELLER);
  }

  @FXML
  private void onCompleted() {
    SceneNavigator.navigateTo(SceneNavigator.View.SELLER);
  }

  @FXML
  private void onProfile() {
    SceneNavigator.navigateTo(SceneNavigator.View.PROFILE);
  }

  @FXML
  private void onLogout() {
    UserSession.getInstance().clear();
    SceneNavigator.navigateTo(SceneNavigator.View.LOGIN);
  }

  private boolean validateAll() {
    boolean ok = true;

    if (titleField.getText().trim().isEmpty()) {
      showError(titleError, titleField, "Item title is required");
      ok = false;
    } else if (titleField.getText().trim().length() < 5) {
      showError(titleError, titleField, "Title must be at least 5 characters");
      ok = false;
    }

    if (categoryCombo.getValue() == null) {
      categoryError.setText("Please select a category");
      categoryError.setVisible(true);
      ok = false;
    }

    if (descriptionField.getText().trim().isEmpty()) {
      showError(descriptionError, descriptionField, "Description is required");
      ok = false;
    }

    final String priceStr = startPriceField.getText().trim();
    if (priceStr.isEmpty()) {
      showError(startPriceError, startPriceField, "Starting price is required");
      ok = false;
    } else {
      try {
        final double price = Double.parseDouble(priceStr);
        if (price <= 0) {
          showError(startPriceError, startPriceField, "Price must be greater than 0");
          ok = false;
        }
      } catch (NumberFormatException e) {
        showError(startPriceError, startPriceField, "Enter a valid number");
        ok = false;
      }
    }

    final String incStr = incrementField.getText().trim();
    if (incStr.isEmpty()) {
      showError(incrementError, incrementField, "Minimum increment is required");
      ok = false;
    } else {
      try {
        final double inc = Double.parseDouble(incStr);
        if (inc <= 0) {
          showError(incrementError, incrementField, "Increment must be greater than 0");
          ok = false;
        }
      } catch (NumberFormatException e) {
        showError(incrementError, incrementField, "Enter a valid number");
        ok = false;
      }
    }

    if (startDatePicker.getValue() == null
        || startHourCombo.getValue() == null
        || startMinuteCombo.getValue() == null) {
      startTimeError.setText("Start date and time are required");
      startTimeError.setVisible(true);
      ok = false;
    }

    if (endDatePicker.getValue() == null
        || endHourCombo.getValue() == null
        || endMinuteCombo.getValue() == null) {
      endTimeError.setText("End date and time are required");
      endTimeError.setVisible(true);
      ok = false;
    }

    return ok;
  }

  private void showError(final Label label, final Control field, final String msg) {
    label.setText(msg);
    label.setVisible(true);
    field.getStyleClass().remove("error");
    field.getStyleClass().add("error");
    generalError.setText("");
  }

  private void clearError(final Label label, final Control field) {
    label.setText("");
    label.setVisible(false);
    field.getStyleClass().remove("error");
  }

  private void clearDateError(final Label label) {
    label.setText("");
    label.setVisible(false);
  }

  private String[] buildHourOptions() {
    final String[] hours = new String[24];
    for (int i = 0; i < 24; i++) {
      hours[i] = String.format("%02d", i);
    }
    return hours;
  }

  private String[] buildMinuteOptions() {
    return new String[]{"00", "15", "30", "45"};
  }

  private String getDateTimeString(final javafx.scene.control.DatePicker datePicker,
      final ComboBox<String> hourCombo, final ComboBox<String> minuteCombo) {
    if (datePicker.getValue() == null
        || hourCombo.getValue() == null
        || minuteCombo.getValue() == null) {
      return null;
    }
    try {
      final int hour = Integer.parseInt(hourCombo.getValue());
      final int minute = Integer.parseInt(minuteCombo.getValue());
      final LocalDate date = datePicker.getValue();
      final LocalTime time = LocalTime.of(hour, minute);
      return date.atTime(time).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    } catch (DateTimeParseException | NumberFormatException ex) {
      return null;
    }
  }

  private void showGeneralError(final String msg) {
    generalError.setStyle("-fx-text-fill:#E53238;-fx-font-size:12px;");
    generalError.setText(msg);
  }

  @FXML
  private void onToggleTheme() {
    SceneNavigator.toggleTheme();
  }

  @FXML
  private void onWallet() {
    SceneNavigator.navigateTo(SceneNavigator.View.WALLET);
  }
}