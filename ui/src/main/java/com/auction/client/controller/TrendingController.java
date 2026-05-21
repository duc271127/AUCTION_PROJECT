package com.auction.client.controller;

import com.auction.client.navigation.SceneManager;
import com.auction.client.session.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class TrendingController {

    @FXML private Label usernameLabel;

    @FXML
    public void initialize() {
        if (!SessionManager.hasRole("BIDDER")) {
            SceneManager.goToAuth();
            return;
        }

        usernameLabel.setText(
                SessionManager.getUsername() == null || SessionManager.getUsername().isBlank()
                        ? "Bidder"
                        : SessionManager.getUsername()
        );
    }

    @FXML
    private void handleGoToForYou() {
        SceneManager.goToShowroom();
    }

    @FXML
    private void handleGoToTrending() {
    }

    @FXML
    private void handleOpenCategories() {
        SceneManager.goToCategory();
    }

    @FXML
    private void handleOpenWallet() {
        SceneManager.goToWallet();
    }

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        SceneManager.goToAuth();
    }

    @FXML
    private void handleGoToArt() {
        SceneManager.goToCategory("Art");
    }

    @FXML
    private void handleGoToJewellery() {
        SceneManager.goToCategory("Jewellery");
    }

    @FXML
    private void handleGoToWatches() {
        SceneManager.goToCategory("Watches");
    }

    @FXML
    private void handleGoToFashion() {
        SceneManager.goToCategory("Fashion");
    }
}
