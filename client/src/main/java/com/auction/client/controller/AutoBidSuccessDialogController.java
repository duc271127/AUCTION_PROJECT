package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class AutoBidSuccessDialogController {

    @FXML private Label successMessageLabel;

    private Stage dialogStage;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setMaxAmount(double maxAmount) {
        successMessageLabel.setText(
                "We'll automatically bid on your behalf up to " + formatMoney(maxAmount) + "."
        );
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
