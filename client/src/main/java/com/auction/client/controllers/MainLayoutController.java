package com.auction.client.controllers;

import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.TopNavUtils;
import com.auction.client.utils.UserSession;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class MainLayoutController implements Initializable {

    @FXML private Button watchlistBtn;
    @FXML private Button myBidsBtn;
    @FXML private Button adminBtn;
    @FXML private Button sellBtn;
    @FXML private Label navWalletBalanceLabel;
    @FXML private Label userLabel;
    @FXML private StackPane contentArea;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        updateNavigation();
        TopNavUtils.bindWalletBalance(navWalletBalanceLabel);
    }

    public void updateNavigation() {
        if (userLabel != null) {
            userLabel.setText(UserSession.getInstance().getInitials());
        }

        boolean isAdmin = UserSession.getInstance().isAdmin();
        boolean isSeller = UserSession.getInstance().isSeller();

        if (adminBtn != null) {
            adminBtn.setVisible(isAdmin);
            adminBtn.setManaged(isAdmin);
        }

        // Hide buyer actions for admin
        if (watchlistBtn != null) {
            watchlistBtn.setVisible(!isAdmin);
            watchlistBtn.setManaged(!isAdmin);
        }
        if (myBidsBtn != null) {
            myBidsBtn.setVisible(!isAdmin);
            myBidsBtn.setManaged(!isAdmin);
        }
        if (sellBtn != null) {
            sellBtn.setVisible(!isAdmin);
            sellBtn.setManaged(!isAdmin);
        }
    }

    public StackPane getContentArea() {
        return contentArea;
    }

    @FXML private void onHome() { SceneNavigator.navigateTo(SceneNavigator.View.HOME); }
    @FXML private void onWatchlist() {
        UserSession.getInstance().setPendingMyBidsFilter("watching");
        SceneNavigator.navigateTo(SceneNavigator.View.MY_BIDS);
    }
    @FXML private void onMyBids() { SceneNavigator.navigateTo(SceneNavigator.View.MY_BIDS); }
    @FXML private void onAdmin() { SceneNavigator.navigateTo(SceneNavigator.View.ADMIN); }
    @FXML private void onSell() { SceneNavigator.navigateTo(SceneNavigator.View.SELLER); }
    @FXML private void onToggleTheme() { SceneNavigator.toggleTheme(); }
    @FXML private void onWallet() { SceneNavigator.navigateTo(SceneNavigator.View.WALLET); }
    @FXML private void onProfile() { SceneNavigator.navigateTo(SceneNavigator.View.PROFILE); }
    @FXML private void onLogout() { 
        UserSession.getInstance().clear(); 
        SceneNavigator.navigateTo(SceneNavigator.View.LOGIN); 
    }
}
