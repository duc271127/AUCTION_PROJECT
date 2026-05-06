package com.auction.client.controller;

import com.auction.client.navigation.SceneManager;
import javafx.fxml.FXML;

public class HomeController {

    @FXML
    private void handleGoToAuth() {
        SceneManager.goToAuth();
    }

    @FXML
    private void handleGoToShowroom() {
        SceneManager.goToShowroom();
    }
}