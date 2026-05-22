package com.auction.client.controller;

import com.auction.client.dto.response.AuctionListResponse;
import com.auction.client.dto.response.BidResponse;
import com.auction.client.service.AuctionApiService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BidHistoryDialogController {

    @FXML private Label auctionTitleLabel;
    @FXML private Label messageLabel;

    @FXML private LineChart<String, Number> bidLineChart;
    @FXML private TableView<BidHistoryRow> bidTableView;
    @FXML private TableColumn<BidHistoryRow, String> timeColumn;
    @FXML private TableColumn<BidHistoryRow, String> bidderColumn;
    @FXML private TableColumn<BidHistoryRow, String> amountColumn;

    private final AuctionApiService auctionApiService = new AuctionApiService();

    private Stage dialogStage;
    private AuctionListResponse auction;
    private boolean demoMode;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setAuction(AuctionListResponse auction, boolean demoMode) {
        this.auction = auction;
        this.demoMode = demoMode;

        bindAuctionTitle();
        loadBidHistory();
    }

    @FXML
    public void initialize() {
        timeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().time()));
        bidderColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().bidder()));
        amountColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().amount()));

        bidTableView.setItems(FXCollections.observableArrayList());
        bidLineChart.setTitle("");
        hideMessage();
    }

    private void bindAuctionTitle() {
        if (auction == null) {
            auctionTitleLabel.setText("Auction");
            return;
        }

        auctionTitleLabel.setText(firstNonBlank(
                auction.getTitle(),
                auction.getItemName(),
                "Auction"
        ));
    }

    private void loadBidHistory() {
        if (auction == null) {
            showMessage("Auction data is unavailable.");
            bindRows(createDemoBids());
            return;
        }

        if (demoMode || auction.getId() == null) {
            bindRows(createDemoBids());
            return;
        }

        CompletableFuture
                .supplyAsync(() -> auctionApiService.getBidHistory(auction.getId().toString()))
                .thenAccept(bids -> runOnUiThread(() -> {
                    if (bids == null || bids.isEmpty()) {
                        bindRows(createDemoBids());
                        showMessage("No bid history from server. Showing demo chart.");
                    } else {
                        bindRows(bids);
                    }
                }))
                .exceptionally(error -> {
                    runOnUiThread(() -> {
                        bindRows(createDemoBids());
                        showMessage("Cannot load bid history. Showing demo chart.");
                    });
                    return null;
                });
    }

    private void bindRows(List<BidResponse> bids) {
        List<BidResponse> sorted = new ArrayList<>(bids);
        sorted.sort(Comparator.comparing(this::safeCreatedAt));

        List<BidHistoryRow> rows = new ArrayList<>();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Bid Amount");

        for (BidResponse bid : sorted) {
            String time = formatTime(bid.getCreatedAt());
            String bidder = firstNonBlank(
                    bid.getBidderName(),
                    bid.isAutoBid() ? "Auto-Bid" : "Unknown bidder"
            );
            String amount = formatMoney(bid.getAmount());

            rows.add(new BidHistoryRow(time, bidder, amount));
            series.getData().add(new XYChart.Data<>(time, bid.getAmount()));
        }

        bidTableView.setItems(FXCollections.observableArrayList(rows));

        bidLineChart.getData().clear();
        bidLineChart.getData().add(series);
    }

    private List<BidResponse> createDemoBids() {
        List<BidResponse> demo = new ArrayList<>();

        double base = auction == null ? 750 : Math.max(750, auction.getCurrentPrice() - 400);
        String[] bidders = {"User A", "User B", "User C", "User D", "User E", "User F"};
        String[] times = {
                "2026-05-20T10:00:00Z",
                "2026-05-20T10:15:00Z",
                "2026-05-20T10:32:00Z",
                "2026-05-20T11:05:00Z",
                "2026-05-20T11:28:00Z",
                "2026-05-20T11:45:00Z"
        };

        for (int i = 0; i < bidders.length; i++) {
            BidResponse bid = new BidResponse();
            bid.setBidderName(bidders[i]);
            bid.setAmount(base + i * 100);
            bid.setCreatedAt(times[i]);
            demo.add(bid);
        }

        return demo;
    }

    @FXML
    private void handleClose() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private Instant safeCreatedAt(BidResponse bid) {
        try {
            if (bid.getCreatedAt() != null && !bid.getCreatedAt().isBlank()) {
                return Instant.parse(bid.getCreatedAt());
            }
        } catch (Exception ignored) {
        }

        return Instant.EPOCH;
    }

    private String formatTime(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }

        try {
            return DateTimeFormatter.ofPattern("HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.parse(value));
        } catch (Exception ignored) {
        }

        try {
            String normalized = value.trim().replace(" ", "T");
            if (normalized.length() > 19) {
                normalized = normalized.substring(0, 19);
            }

            LocalDateTime dateTime = LocalDateTime.parse(normalized);
            return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception ignored) {
        }

        return value.length() >= 5 ? value.substring(0, 5) : value;
    }

    private String formatMoney(double value) {
        return "€ " + String.format("%,.0f", value);
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

    private void showMessage(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void hideMessage() {
        if (messageLabel == null) {
            return;
        }

        messageLabel.setText("");
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    private void runOnUiThread(Runnable task) {
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }

    public record BidHistoryRow(String time, String bidder, String amount) {
    }
}