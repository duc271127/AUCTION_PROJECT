module com.auction.client {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.auction.client to javafx.graphics;
    opens com.auction.client.controller to javafx.fxml;

    exports com.auction.client;
}
