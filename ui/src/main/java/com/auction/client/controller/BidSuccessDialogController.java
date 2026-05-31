package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class BidSuccessDialogController {

    @FXML private Label successMessageLabel;

    private Stage dialogStage;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setBidAmount(double amount) {
        successMessageLabel.setText("Your bid of " + formatMoney(amount) + " has been confirmed.");
    }

    @FXML
    private void handleClose() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private String formatMoney(double value) {
        return "USD " + String.format("%,.0f", value);
    }
}
