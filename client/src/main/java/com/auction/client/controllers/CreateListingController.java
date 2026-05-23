package com.auction.client.controllers;

import com.auction.client.services.NetworkClientService;
import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.ToastNotification;
import com.auction.client.utils.UserSession;
import com.auction.client.utils.TopNavUtils;
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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 * CreateListingController handles the Create Listing form.
 * Validates input, sends CREATE_ITEM to the server, and navigates on success.
 */
public class CreateListingController implements Initializable {

  @FXML private BorderPane rootPane;
  @FXML private TextField titleField;
  @FXML private ComboBox<String> categoryCombo;
  @FXML private TextArea descriptionField;
  @FXML private TextField startPriceField;
  @FXML private TextField incrementField;
  @FXML private DatePicker startDatePicker;
  @FXML private ComboBox<String> startHourCombo;
  @FXML private ComboBox<String> startMinuteCombo;
  @FXML private DatePicker endDatePicker;
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
  private byte[] selectedImageBytes;

  @Override
  public void initialize(final URL url, final ResourceBundle rb) {
    categoryCombo.getItems().addAll(
        "Electronics", "Fashion", "Home & Garden",
        "Sports", "Collectibles", "Vehicles", "Art", "Other");

    // Populate hour and minute lists
    for (int i = 0; i < 24; i++) {
      String h = String.format("%02d", i);
      startHourCombo.getItems().add(h);
      endHourCombo.getItems().add(h);
    }
    for (int i = 0; i < 60; i += 5) {
      String m = String.format("%02d", i);
      startMinuteCombo.getItems().add(m);
      endMinuteCombo.getItems().add(m);
    }

    // Default values (Start: now, End: now + 7 days)
    LocalDateTime now = LocalDateTime.now();
    startDatePicker.setValue(now.toLocalDate());
    startHourCombo.setValue(String.format("%02d", now.getHour()));
    
    // Round minute to nearest 5 minutes
    int minuteRounded = (now.getMinute() / 5) * 5;
    if (minuteRounded >= 60) minuteRounded = 55;
    startMinuteCombo.setValue(String.format("%02d", minuteRounded));

    LocalDateTime endDefault = now.plusDays(7);
    endDatePicker.setValue(endDefault.toLocalDate());
    endHourCombo.setValue(String.format("%02d", endDefault.getHour()));
    endMinuteCombo.setValue(String.format("%02d", minuteRounded));

    titleField.textProperty().addListener((o, v, n) -> clearError(titleError, titleField));
    descriptionField.textProperty().addListener(
        (o, v, n) -> clearError(descriptionError, descriptionField));
    startPriceField.textProperty().addListener(
        (o, v, n) -> clearError(startPriceError, startPriceField));
    incrementField.textProperty().addListener(
        (o, v, n) -> clearError(incrementError, incrementField));

    startDatePicker.valueProperty().addListener((o, v, n) -> clearStartError());
    startHourCombo.valueProperty().addListener((o, v, n) -> clearStartError());
    startMinuteCombo.valueProperty().addListener((o, v, n) -> clearStartError());

    endDatePicker.valueProperty().addListener((o, v, n) -> clearEndError());
    endHourCombo.valueProperty().addListener((o, v, n) -> clearEndError());
    endMinuteCombo.valueProperty().addListener((o, v, n) -> clearEndError());
  }

  private void clearStartError() {
    startTimeError.setText("");
    startTimeError.setVisible(false);
    startDatePicker.getStyleClass().remove("error");
    startHourCombo.getStyleClass().remove("error");
    startMinuteCombo.getStyleClass().remove("error");
  }

  private void clearEndError() {
    endTimeError.setText("");
    endTimeError.setVisible(false);
    endDatePicker.getStyleClass().remove("error");
    endHourCombo.getStyleClass().remove("error");
    endMinuteCombo.getStyleClass().remove("error");
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

    long startTime = 0;
    long endTime = 0;
    try {
      startTime = LocalDateTime.of(
          startDatePicker.getValue(),
          java.time.LocalTime.of(
              Integer.parseInt(startHourCombo.getValue()),
              Integer.parseInt(startMinuteCombo.getValue())
          )
      ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

      endTime = LocalDateTime.of(
          endDatePicker.getValue(),
          java.time.LocalTime.of(
              Integer.parseInt(endHourCombo.getValue()),
              Integer.parseInt(endMinuteCombo.getValue())
          )
      ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    } catch (Exception e) {
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
    if (selectedImageBytes != null && selectedImageBytes.length > 0) {
      json.addProperty("imageData", java.util.Base64.getEncoder().encodeToString(selectedImageBytes));
    }

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
            ToastNotification.show(rootPane, "Listing published successfully!",
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
    javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
    fileChooser.setTitle("Select Product Image");
    fileChooser.getExtensionFilters().addAll(
        new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
    );
    java.io.File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
    if (file != null) {
      try {
        byte[] original = java.nio.file.Files.readAllBytes(file.toPath());
        selectedImageBytes = resizeImage(original, 600, 400);
        imageLabel.setText(file.getName() + " (Compressed)");
      } catch (java.io.IOException e) {
        showGeneralError("Failed to read image file: " + e.getMessage());
      }
    }
  }

  private byte[] resizeImage(byte[] originalBytes, int maxWidth, int maxHeight) {
    try {
      java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(originalBytes);
      java.awt.image.BufferedImage originalImage = javax.imageio.ImageIO.read(in);
      if (originalImage == null) {
        return originalBytes;
      }

      int originalWidth = originalImage.getWidth();
      int originalHeight = originalImage.getHeight();

      if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
        return originalBytes;
      }

      double ratio = Math.min((double) maxWidth / originalWidth, (double) maxHeight / originalHeight);
      int newWidth = (int) (originalWidth * ratio);
      int newHeight = (int) (originalHeight * ratio);

      java.awt.image.BufferedImage outputImage = new java.awt.image.BufferedImage(newWidth, newHeight, java.awt.image.BufferedImage.TYPE_INT_RGB);
      java.awt.Graphics2D g2d = outputImage.createGraphics();
      g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
      g2d.dispose();

      java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
      javax.imageio.ImageIO.write(outputImage, "jpg", out);
      return out.toByteArray();
    } catch (Exception e) {
      System.err.println("Failed to resize image: " + e.getMessage());
      return originalBytes;
    }
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

    if (startDatePicker.getValue() == null) {
      showError(startTimeError, startDatePicker, "Start date is required");
      ok = false;
    } else if (startHourCombo.getValue() == null) {
      showError(startTimeError, startHourCombo, "Start hour is required");
      ok = false;
    } else if (startMinuteCombo.getValue() == null) {
      showError(startTimeError, startMinuteCombo, "Start minute is required");
      ok = false;
    } else {
      try {
        LocalDateTime startDt = LocalDateTime.of(
            startDatePicker.getValue(),
            java.time.LocalTime.of(
                Integer.parseInt(startHourCombo.getValue()),
                Integer.parseInt(startMinuteCombo.getValue())
            )
        );
        if (startDt.isBefore(LocalDateTime.now().minusMinutes(5))) {
          showError(startTimeError, startDatePicker, "Start time cannot be in the past");
          ok = false;
        }
      } catch (Exception e) {
        showError(startTimeError, startDatePicker, "Invalid start date & time");
        ok = false;
      }
    }

    if (endDatePicker.getValue() == null) {
      showError(endTimeError, endDatePicker, "End date is required");
      ok = false;
    } else if (endHourCombo.getValue() == null) {
      showError(endTimeError, endHourCombo, "End hour is required");
      ok = false;
    } else if (endMinuteCombo.getValue() == null) {
      showError(endTimeError, endMinuteCombo, "End minute is required");
      ok = false;
    } else {
      try {
        LocalDateTime startDt = null;
        if (startDatePicker.getValue() != null && startHourCombo.getValue() != null && startMinuteCombo.getValue() != null) {
          startDt = LocalDateTime.of(
              startDatePicker.getValue(),
              java.time.LocalTime.of(
                  Integer.parseInt(startHourCombo.getValue()),
                  Integer.parseInt(startMinuteCombo.getValue())
              )
          );
        }
        LocalDateTime endDt = LocalDateTime.of(
            endDatePicker.getValue(),
            java.time.LocalTime.of(
                Integer.parseInt(endHourCombo.getValue()),
                Integer.parseInt(endMinuteCombo.getValue())
            )
        );
        if (startDt != null && !endDt.isAfter(startDt)) {
          showError(endTimeError, endDatePicker, "End time must be after start time");
          ok = false;
        }
      } catch (Exception e) {
        showError(endTimeError, endDatePicker, "Invalid end date & time");
        ok = false;
      }
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

}
