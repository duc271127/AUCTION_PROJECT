module com.auction.client {
    requires org.java_websocket;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.google.gson;
    requires spring.websocket;
    requires spring.messaging;

    opens com.auction.client to javafx.fxml;
    opens com.auction.client.controller to javafx.fxml;
    opens com.auction.client.model to javafx.base;

    opens com.auction.client.dto.request to com.fasterxml.jackson.databind, com.google.gson;
    opens com.auction.client.dto.response to com.fasterxml.jackson.databind, com.google.gson;
    opens com.auction.client.dto.event to com.fasterxml.jackson.databind, com.google.gson;

    exports com.auction.client;
    exports com.auction.client.controller;
    exports com.auction.client.model;
    exports com.auction.client.dto.event;
    exports com.auction.client.socket;
}