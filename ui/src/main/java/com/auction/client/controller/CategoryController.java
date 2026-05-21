package com.auction.client.controller;

import com.auction.client.navigation.SceneManager;
import com.auction.client.session.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class CategoryController {

    @FXML
    private Label usernameLabel;

    @FXML
    private Label categoryTitleLabel;

    @FXML
    private void initialize() {
        if (usernameLabel != null) {
            usernameLabel.setText(SessionManager.getUsername());
        }
        categoryTitleLabel.setText(selectedCategory);
    }

    @FXML
    private void handleGoToForYou() {
        SceneManager.goToShowroom();
    }

    @FXML
    private void handleGoToTrending() {
        SceneManager.goToTrending();
    }

    @FXML
    private void handleOpenWallet() {
        SceneManager.goToWallet();
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

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        SceneManager.goToAuth();
    }

    private static String selectedCategory = "Art";

    public static void setSelectedCategory(String category) {
        selectedCategory = category;
    }
}
