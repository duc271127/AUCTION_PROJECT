package com.auction.client.navigation;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneManager {

    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static void switchScene(String fxmlPath, String title, String... cssPaths) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root);

            scene.getStylesheets().add(
                    SceneManager.class.getResource("/css/app.css").toExternalForm()
            );

            scene.getStylesheets().add(
                    SceneManager.class.getResource("/css/components.css").toExternalForm()
            );

            for (String cssPath : cssPaths) {
                scene.getStylesheets().add(
                        SceneManager.class.getResource(cssPath).toExternalForm()
                );
            }

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
        switchScene("/fxml/auth.fxml", "Auction Project - Auth", "/css/auth.css");
    }

    public static void goToSellerDashboard() {
        switchScene("/fxml/seller_dashboard.fxml", "Auction Project - Seller Dashboard", "/css/seller.css");
    }

    public static void goToAdminDashboard() {
        switchScene("/fxml/admin_dashboard.fxml", "Auction Project - Admin Dashboard", "/css/admin.css");
    }

    public static void goToProductDetail() {
        switchScene("/fxml/product_detail.fxml", "Auction Project - Product Detail", "/css/product_detail.css");
    }

    public static void goToShowroom() {
        switchScene("/fxml/showroom.fxml", "Auction Project - Showroom", "/css/showroom.css");
    }

    public static void goToHome() {
        switchScene("/fxml/home.fxml", "Auction Project - Home", "/css/home.css");
    }

    public static void goToLiveBidding() {
        switchScene("/fxml/live_bidding.fxml", "Auction Project - Live Bidding", "/css/live_bidding.css");
    }
}