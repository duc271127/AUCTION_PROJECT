package com.auction.client.controller;

import com.auction.client.dto.response.LoginResponse;
import com.auction.client.dto.response.RegisterResponse;
import com.auction.client.navigation.SceneManager;
import com.auction.client.service.AuthApiService;
import com.auction.client.session.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;

public class AuthController {

    @FXML private Label loginErrorLabel;
    @FXML private Label registerErrorLabel;
    @FXML private TabPane authTabPane;

    @FXML private TextField loginUsernameField;
    @FXML private PasswordField loginPasswordField;
    @FXML private ComboBox<String> loginRoleComboBox;

    @FXML private TextField registerUsernameField;
    @FXML private TextField registerEmailField;
    @FXML private PasswordField registerPasswordField;
    @FXML private PasswordField registerConfirmPasswordField;
    @FXML private ComboBox<String> registerRoleComboBox;

    private final AuthApiService authApiService = new AuthApiService();

    @FXML
    public void initialize() {
        loginRoleComboBox.setItems(FXCollections.observableArrayList("Bidder", "Seller", "Admin"));
        registerRoleComboBox.setItems(FXCollections.observableArrayList("Bidder", "Seller"));

        loginRoleComboBox.setValue("Bidder");
        registerRoleComboBox.setValue("Bidder");

        loginErrorLabel.setText("");
        loginErrorLabel.setManaged(true);
        loginErrorLabel.setVisible(true);

        registerErrorLabel.setText("");
        registerErrorLabel.setManaged(true);
        registerErrorLabel.setVisible(true);
    }

    @FXML
    private void handleBack() {
        SceneManager.goToHome();
    }

    @FXML
    private void handleLogin() {
        hideLoginError();

        String credential = loginUsernameField.getText().trim();
        String password = loginPasswordField.getText().trim();
        String selectedRole = loginRoleComboBox.getValue();

        if (credential.isEmpty()) {
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

        if (selectedRole == null || selectedRole.isEmpty()) {
            showLoginError("Please select a role.");
            return;
        }

        try {
            LoginResponse response = authApiService.login(credential, password);

            if (response.getRole() == null || response.getRole().isBlank()) {
                showLoginError("Role returned from server is empty.");
                return;
            }

            String selectedNormalizedRole = SessionManager.normalizeRole(selectedRole);
            String responseNormalizedRole = SessionManager.normalizeRole(response.getRole());

            if (!selectedNormalizedRole.equals(responseNormalizedRole)) {
                showLoginError("Selected role does not match this account.");
                return;
            }

            String sessionDisplayName = firstNonBlank(
                    response.getUsername(),
                    response.getEmail(),
                    credential
            );

            SessionManager.setUsername(sessionDisplayName);
            SessionManager.setRole(responseNormalizedRole);
            SessionManager.setUserId(response.getId());

            if (response.getToken() != null && !response.getToken().isBlank()) {
                SessionManager.setToken(response.getToken());
            }

            navigateByRole(responseNormalizedRole);

        } catch (Exception e) {
            showLoginError(toFriendlyError(e.getMessage()));
        }
    }

    @FXML
    private void showLoginForm() {
        authTabPane.getSelectionModel().select(0);
        hideLoginError();
        hideRegisterError();
    }

    @FXML
    private void showRegisterForm() {
        authTabPane.getSelectionModel().select(1);
        hideLoginError();
        hideRegisterError();
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

        try {
            RegisterResponse response = authApiService.register(
                    username,
                    email,
                    password,
                    role.toUpperCase()
            );

            String identity = firstNonBlank(
                    response.getUsername(),
                    response.getEmail(),
                    username,
                    email
            );

            loginUsernameField.setText(firstNonBlank(email, username));
            loginPasswordField.clear();
            loginRoleComboBox.setValue(toDisplayRole(firstNonBlank(response.getRole(), role)));
            showLoginForm();
            showLoginSuccess("Register successful: " + identity + ". Please sign in.");

        } catch (Exception e) {
            showRegisterError(toFriendlyError(e.getMessage()));
        }
    }

    private void navigateByRole(String role) {
        if (role == null) {
            showLoginError("Role returned from server is empty.");
            return;
        }

        switch (SessionManager.normalizeRole(role)) {
            case "BIDDER" -> SceneManager.goToShowroom();
            case "SELLER" -> SceneManager.goToSellerDashboard();
            case "ADMIN" -> SceneManager.goToAdminDashboard();
            default -> showLoginError("Invalid role returned from server.");
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private void showLoginError(String message) {
        loginErrorLabel.setText(message);
        loginErrorLabel.setStyle("-fx-text-fill: #dc2626;");
    }

    private void showLoginSuccess(String message) {
        loginErrorLabel.setText(message);
        loginErrorLabel.setStyle("-fx-text-fill: #16a34a;");
    }

    private void hideLoginError() {
        loginErrorLabel.setText("");
    }

    private void showRegisterError(String message) {
        registerErrorLabel.setText(message);
        registerErrorLabel.setStyle("-fx-text-fill: #dc2626;");
    }

    private void hideRegisterError() {
        registerErrorLabel.setText("");
    }

    private String toDisplayRole(String role) {
        return switch (SessionManager.normalizeRole(role)) {
            case "SELLER" -> "Seller";
            case "ADMIN" -> "Admin";
            default -> "Bidder";
        };
    }

    private String toFriendlyError(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown error.";
        }

        if (message.contains("Cannot connect to server")) {
            return message + ". Make sure backend is reachable at lungs-decree.with.playit.plus:1125.";
        }

        return message;
    }
}
