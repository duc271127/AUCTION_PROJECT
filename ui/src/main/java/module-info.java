module com.auction.client {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.auction.client to javafx.fxml;
    opens com.auction.client.controller to javafx.fxml;
    opens com.auction.client.model to javafx.base;

    exports com.auction.client;
    exports com.auction.client.controller;
    exports com.auction.client.model;
}