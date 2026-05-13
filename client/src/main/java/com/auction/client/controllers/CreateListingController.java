package com.auction.client.controllers;

import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.UserSession;
import java.net.URL;
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

    // TODO: AuctionService.createAuction(...)
    // AuctionService.create(title, category, description,
    //         startPrice, increment, startTime, endTime)
    //         .onSuccess(auction -> SceneNavigator.navigateTo(View.HOME))
    //         .onFailure(err -> showGeneralError(err.getMessage()));
    
    System.out.println("Publish: " + titleField.getText());
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
    SceneNavigator.navigateTo(SceneNavigator.View.HOME);
  }

  @FXML
  private void onHome() {
    SceneNavigator.navigateTo(SceneNavigator.View.HOME);
  }

  @FXML
  private void onMyListings() {
    System.out.println("TODO: MyListingsView");
  }

  @FXML
  private void onActiveAuctions() {
    System.out.println("TODO: ActiveAuctionsView");
  }

  @FXML
  private void onDrafts() {
    System.out.println("TODO: DraftsView");
  }

  @FXML
  private void onCompleted() {
    System.out.println("TODO: CompletedView");
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
}