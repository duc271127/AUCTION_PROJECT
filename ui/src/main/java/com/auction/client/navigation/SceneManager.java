package com.auction.client.navigation;

import com.auction.client.session.SessionManager;
import com.auction.client.controller.CategoryController;
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
            System.out.println("Switching scene to: " + fxmlPath);

            var fxmlUrl = SceneManager.class.getResource(fxmlPath);
            if (fxmlUrl == null) {
                throw new RuntimeException("FXML not found: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Scene scene = new Scene(root);

            addCss(scene, "/css/app.css");
            addCss(scene, "/css/components.css");

            for (String cssPath : cssPaths) {
                addCss(scene, cssPath);
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

            System.out.println("Scene switched successfully: " + fxmlPath);

        } catch (Exception e) {
            System.err.println("Cannot switch scene to: " + fxmlPath);
            e.printStackTrace();
        }
    }

    private static void addCss(Scene scene, String cssPath) {
        var cssUrl = SceneManager.class.getResource(cssPath);
        if (cssUrl == null) {
            System.err.println("CSS not found, skipped: " + cssPath);
            return;
        }
        scene.getStylesheets().add(cssUrl.toExternalForm());
    }

    public static void goToAuth() {
        switchScene("/fxml/auth.fxml", "Auction Project - Auth", "/css/auth.css");
    }

    public static void goToSellerDashboard() {
        if (!requireRole("SELLER")) {
            return;
        }
        switchScene("/fxml/seller_dashboard.fxml", "Auction Project - Seller Dashboard", "/css/seller.css");
    }

    public static void goToAdminDashboard() {
        if (!requireRole("ADMIN")) {
            return;
        }
        switchScene("/fxml/admin_dashboard.fxml", "Auction Project - Admin Dashboard", "/css/admin.css");
    }

    public static void goToProductDetail() {
        if (!requireRole("BIDDER")) {
            return;
        }
        switchScene("/fxml/product_detail.fxml", "Auction Project - Product Detail", "/css/product_detail.css");
    }

    public static void goToShowroom() {
        if (!requireRole("BIDDER")) {
            return;
        }
        switchScene("/fxml/showroom.fxml", "Auction Project - Showroom", "/css/showroom.css");
    }

    public static void goToTrending() {
        if (!requireRole("BIDDER")) {
            return;
        }
        switchScene(
                "/fxml/trending.fxml", "Auction Project - Trending", "/css/trending.css"
        );
    }

    public static void goToHome() {
        switchScene("/fxml/home.fxml", "Auction Project - Home", "/css/home.css");
    }

    public static void goToLiveBidding() {
        if (!requireRole("BIDDER")) {
            return;
        }
        switchScene("/fxml/live_bidding.fxml", "Auction Project - Live Bidding", "/css/live_bidding.css");
    }

    public static void goToWallet() {
        if (!SessionManager.isAuthenticated()) {
            goToAuth();
            return;
        }
        switchScene("/fxml/wallet.fxml", "Auction Project - Wallet", "/css/wallet.css");
    }

    public static void goToWonAuctions() {
        if (!requireRole("BIDDER")) {
            return;
        }
        switchScene("/fxml/won_auctions.fxml", "Auction Project - Won Auctions", "/css/won_auctions.css");
    }

    public static void goToCategory() {
        if (!requireRole("BIDDER")) {
            return;
        }

        switchScene("/fxml/category.fxml", "Auction Project - Category", "/css/category.css"
        );
    }

    public static void goToCategory(String category) {
        if (!requireRole("BIDDER")) {
            return;
        }

        CategoryController.setSelectedCategory(category);

        switchScene(
                "/fxml/category.fxml",
                "Auction Project - " + category,
                "/css/category.css"
        );
    }

    private static boolean requireRole(String role) {
        if (SessionManager.hasRole(role)) {
            return true;
        }

        if (primaryStage != null) {
            switchScene("/fxml/auth.fxml", "Auction Project - Auth", "/css/auth.css");
        }
        return false;
    }
}
