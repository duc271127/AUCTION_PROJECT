package com.auction.client.controller;

import com.auction.client.navigation.SceneManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AuthController {

    @FXML private Label loginErrorLabel;
    @FXML private Label registerErrorLabel;

    @FXML private TextField loginUsernameField;
    @FXML private PasswordField loginPasswordField;
    @FXML private ComboBox<String> loginRoleComboBox;

    @FXML private TextField registerUsernameField;
    @FXML private TextField registerEmailField;
    @FXML private PasswordField registerPasswordField;
    @FXML private PasswordField registerConfirmPasswordField;
    @FXML private ComboBox<String> registerRoleComboBox;

    @FXML
    public void initialize() {
        loginRoleComboBox.setItems(FXCollections.observableArrayList("Bidder", "Seller", "Admin"));
        registerRoleComboBox.setItems(FXCollections.observableArrayList("Bidder", "Seller"));

        loginRoleComboBox.setValue("Bidder");
        registerRoleComboBox.setValue("Bidder");

        loginErrorLabel.setManaged(false);
        loginErrorLabel.setVisible(false);

        registerErrorLabel.setManaged(false);
        registerErrorLabel.setVisible(false);
    }

    @FXML
    private void handleLogin() {
        hideLoginError();

        String username = loginUsernameField.getText().trim();
        String password = loginPasswordField.getText().trim();
        String role = loginRoleComboBox.getValue();

        if (username.isEmpty()) {
            showLoginError("Username or email is required.");
            return;
        }

        if (password.isEmpty()) {
            showLoginError("Password is required.");
            return;
        }

        if (password.length() < 6) {
            showLoginError("Password must be at least 6 characters.");
            return;
        }

        if (role == null || role.isEmpty()) {
            showLoginError("Please select a role.");
            return;
        }

        switch (role) {
            case "Bidder" -> SceneManager.goToShowroom();
            case "Seller" -> SceneManager.goToSellerDashboard();
            case "Admin" -> SceneManager.goToAdminDashboard();
            default -> showLoginError("Invalid role selected.");
        }
    }

    @FXML
    private void handleRegister() {
        hideRegisterError();

        String username = registerUsernameField.getText().trim();
        String email = registerEmailField.getText().trim();
        String password = registerPasswordField.getText().trim();
        String confirmPassword = registerConfirmPasswordField.getText().trim();
        String role = registerRoleComboBox.getValue();

        if (username.isEmpty()) {
            showRegisterError("Username is required.");
            return;
        }

        if (email.isEmpty()) {
            showRegisterError("Email is required.");
            return;
        }

        if (!email.contains("@") || !email.contains(".")) {
            showRegisterError("Email format is invalid.");
            return;
        }

        if (password.isEmpty()) {
            showRegisterError("Password is required.");
            return;
        }

        if (password.length() < 6) {
            showRegisterError("Password must be at least 6 characters.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showRegisterError("Passwords do not match.");
            return;
        }

        if (role == null || role.isEmpty()) {
            showRegisterError("Please select a role.");
            return;
        }

        showRegisterError("Register successful (demo only).");
    }

    private void showLoginError(String message) {
        loginErrorLabel.setText(message);
        loginErrorLabel.setManaged(true);
        loginErrorLabel.setVisible(true);
    }

    private void hideLoginError() {
        loginErrorLabel.setText("");
        loginErrorLabel.setManaged(false);
        loginErrorLabel.setVisible(false);
    }

    private void showRegisterError(String message) {
        registerErrorLabel.setText(message);
        registerErrorLabel.setManaged(true);
        registerErrorLabel.setVisible(true);
    }

    private void hideRegisterError() {
        registerErrorLabel.setText("");
        registerErrorLabel.setManaged(false);
        registerErrorLabel.setVisible(false);
    }
}