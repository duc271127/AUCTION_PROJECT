package com.auction.client.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

import java.io.IOException;

public class SceneManager {

    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static void switchScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            String css = SceneManager.class.getResource("/css/app.css").toExternalForm();
            scene.getStylesheets().add(css);

            primaryStage.setTitle(title);
            primaryStage.setScene(scene);

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

            double safeWidth = Math.min(1400, screenBounds.getWidth() - 40);
            double safeHeight = Math.min(900, screenBounds.getHeight() - 40);

            primaryStage.setWidth(safeWidth);
            primaryStage.setHeight(safeHeight);

            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);

            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void goToAuth() {
        switchScene("/fxml/auth.fxml", "Auction Project - Auth");
    }
    public static void goToSellerDashboard() {
        switchScene("/fxml/seller_dashboard.fxml", "Auction Project - Seller Dashboard");
    }

    public static void goToAdminDashboard() {
        switchScene("/fxml/admin_dashboard.fxml", "Auction Project - Admin Dashboard");
    }
    public static void goToProductDetail() {
        switchScene("/fxml/product_detail.fxml", "Auction Project - Product Detail");
    }
    public static void goToShowroom() {
        switchScene("/fxml/showroom.fxml", "Auction Project - Showroom");
    }
    public static void goToHome() {
        switchScene("/fxml/home.fxml", "Auction Project - Home");
    }
    public static void goToLiveBidding() {
        switchScene("/fxml/live_bidding.fxml", "Auction Project - Live Bidding");
    }
}


