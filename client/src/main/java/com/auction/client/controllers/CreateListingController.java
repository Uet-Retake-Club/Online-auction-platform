package com.auction.client.controllers;

import com.auction.client.services.NetworkClientService;
import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.ToastNotification;
import com.auction.client.utils.UserSession;
import com.auction.shared.dto.MessageType;
import com.auction.shared.dto.Request;
import com.google.gson.JsonObject;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;
import javafx.application.Platform;
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
 * CreateListingController handles the Create Listing form.
 * Validates input, sends CREATE_ITEM to the server, and navigates on success.
 */
public class CreateListingController implements Initializable {

  @FXML private Label userLabel;
  @FXML private TextField titleField;
  @FXML private ComboBox<String> categoryCombo;
  @FXML private TextArea descriptionField;
  @FXML private TextField startPriceField;
  @FXML private TextField incrementField;
  @FXML private TextField startTimeField;
  @FXML private TextField endTimeField;
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

  private static final DateTimeFormatter DT_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  @Override
  public void initialize(final URL url, final ResourceBundle rb) {
    userLabel.setText(UserSession.getInstance().getInitials());

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
    startTimeField.textProperty().addListener(
        (o, v, n) -> clearError(startTimeError, startTimeField));
    endTimeField.textProperty().addListener(
        (o, v, n) -> clearError(endTimeError, endTimeField));
  }

  @FXML
  private void onPublish() {
    if (!validateAll()) {
      return;
    }

    final String title = titleField.getText().trim();
    final String category = categoryCombo.getValue();
    final String description = descriptionField.getText().trim();
    final double startPrice = Double.parseDouble(startPriceField.getText().trim());

    long startTime;
    long endTime;
    try {
      startTime = LocalDateTime.parse(startTimeField.getText().trim(), DT_FMT)
          .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
      endTime = LocalDateTime.parse(endTimeField.getText().trim(), DT_FMT)
          .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    } catch (DateTimeParseException e) {
      startTime = System.currentTimeMillis();
      endTime = System.currentTimeMillis() + 7L * 24 * 3600 * 1000;
    }

    // Build JSON payload
    final JsonObject json = new JsonObject();
    json.addProperty("title", title);
    json.addProperty("category", category);
    json.addProperty("description", description);
    json.addProperty("startPrice", startPrice);
    json.addProperty("startTime", startTime);
    json.addProperty("endTime", endTime);

    final Request req = new Request(MessageType.CREATE_ITEM,
        UserSession.getInstance().getUserId(), json.toString());

    // One-shot listener for the response
    final NetworkClientService.ServerMessageListener[] ref =
        new NetworkClientService.ServerMessageListener[1];
    ref[0] = response -> {
      final MessageType type = response.getType();
      if (type == MessageType.CREATE_ITEM_SUCCESS || type == MessageType.CREATE_ITEM_FAIL) {
        NetworkClientService.getInstance().removeListener(ref[0]);
        Platform.runLater(() -> {
          if (type == MessageType.CREATE_ITEM_SUCCESS) {
            ToastNotification.show(userLabel, "Listing published successfully!",
                ToastNotification.Type.SUCCESS);
            SceneNavigator.navigateTo(SceneNavigator.View.SELLER);
          } else {
            showGeneralError(response.getMessage());
          }
        });
      }
    };
    NetworkClientService.getInstance().addListener(ref[0]);
    NetworkClientService.getInstance().sendRequest(req);

    publishBtn.setDisable(true);
    publishBtn.setText("Publishing...");
  }

  @FXML
  private void onSaveDraft() {
    generalError.setText("Draft saved locally.");
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

  // ── Validation ─────────────────────────────────────────────

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

    if (startTimeField.getText().trim().isEmpty()) {
      showError(startTimeError, startTimeField, "Start time is required");
      ok = false;
    }

    if (endTimeField.getText().trim().isEmpty()) {
      showError(endTimeError, endTimeField, "End time is required");
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

  private void showGeneralError(final String msg) {
    generalError.setStyle("-fx-text-fill:#E53238;-fx-font-size:12px;");
    generalError.setText(msg);
  }

  @FXML
  private void onToggleTheme() {
    SceneNavigator.toggleTheme();
  }
}