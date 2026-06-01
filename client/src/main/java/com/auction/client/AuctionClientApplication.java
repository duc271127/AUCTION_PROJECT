package com.auction.client;

import com.auction.client.navigation.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class AuctionClientApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        SceneManager.setPrimaryStage(primaryStage);
        SceneManager.goToAuth();
    }
}
