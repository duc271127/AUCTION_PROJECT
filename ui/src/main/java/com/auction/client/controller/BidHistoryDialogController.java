package com.auction.client.controller;

import com.auction.client.dto.response.AuctionListResponse;
import com.auction.client.dto.response.BidResponse;
import com.auction.client.service.AuctionApiService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BidHistoryDialogController {
    private static final int VISIBLE_TIME_SLOTS = 6;
    private static final double TIME_SLOT_WIDTH = 74;
    private static final double CHART_EDGE_PADDING = 44;

    @FXML private Label auctionTitleLabel;
    @FXML private Label messageLabel;

    @FXML private LineChart<String, Number> bidLineChart;
    @FXML private CategoryAxis timeAxis;
    @FXML private NumberAxis amountAxis;
    @FXML private ScrollPane chartScrollPane;
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

        timeColumn.setCellFactory(column -> createTableCell(Pos.CENTER_LEFT, "bid-history-time-cell"));
        bidderColumn.setCellFactory(column -> createTableCell(Pos.CENTER_LEFT, "bid-history-bidder-cell"));
        amountColumn.setCellFactory(column -> createTableCell(Pos.CENTER_RIGHT, "bid-history-amount-cell"));

        bidTableView.setItems(FXCollections.observableArrayList());
        bidTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        bidTableView.setPlaceholder(new Label("No bids yet."));

        bidLineChart.setTitle("");
        bidLineChart.setAnimated(false);
        bidLineChart.setLegendVisible(false);
        bidLineChart.setHorizontalGridLinesVisible(true);
        bidLineChart.setVerticalGridLinesVisible(false);
        bidLineChart.setAlternativeColumnFillVisible(false);
        bidLineChart.setAlternativeRowFillVisible(false);
        bidLineChart.setCreateSymbols(true);
        if (chartScrollPane != null) {
            chartScrollPane.setPannable(false);
        }

        if (timeAxis != null) {
            timeAxis.setAnimated(false);
            timeAxis.setLabel("");
            timeAxis.setTickLabelRotation(0);
            timeAxis.setStartMargin(10);
            timeAxis.setEndMargin(10);
        }

        if (amountAxis != null) {
            amountAxis.setAnimated(false);
            amountAxis.setLabel("");
            amountAxis.setForceZeroInRange(false);
            amountAxis.setMinorTickVisible(false);
            amountAxis.setTickLabelFormatter(new StringConverter<>() {
                @Override
                public String toString(Number value) {
                    return formatAxisAmount(value == null ? 0 : value.doubleValue());
                }

                @Override
                public Number fromString(String string) {
                    return 0;
                }
            });
        }

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

        int lastIndex = sorted.size() - 1;
        for (int i = 0; i < sorted.size(); i++) {
            BidResponse bid = sorted.get(i);
            String time = formatTime(bid.getCreatedAt());
            String bidder = firstNonBlank(
                    bid.getBidderName(),
                    bid.isAutoBid() ? "Auto-Bid" : "Unknown bidder"
            );
            String amount = formatMoney(bid.getAmount());

            rows.add(new BidHistoryRow(time, bidder, amount));

            XYChart.Data<String, Number> point = new XYChart.Data<>(time, bid.getAmount());
            series.getData().add(point);
            installPointTooltip(point, bidder, time, bid.getAmount(), i == lastIndex);
        }

        bidTableView.setItems(FXCollections.observableArrayList(rows));
        bidLineChart.getData().clear();
        bidLineChart.getData().add(series);
        updateChartWidth(sorted.size());

        Platform.runLater(() -> {
            styleSeries(series);
            if (chartScrollPane != null) {
                chartScrollPane.setHvalue(0);
            }
        });
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
            return DateTimeFormatter.ofPattern("h:mm a")
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
            return dateTime.format(DateTimeFormatter.ofPattern("h:mm a"));
        } catch (Exception ignored) {
        }

        return value;
    }

    private String formatMoney(double value) {
        return "USD " + String.format("%,.0f", value);
    }

    private String formatAxisAmount(double value) {
        return String.format("%,.0f", value);
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

    private TableCell<BidHistoryRow, String> createTableCell(Pos alignment, String styleClass) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setAlignment(alignment);
                getStyleClass().removeAll("bid-history-time-cell", "bid-history-bidder-cell", "bid-history-amount-cell");
                if (!empty) {
                    getStyleClass().add(styleClass);
                }
            }
        };
    }

    private void installPointTooltip(XYChart.Data<String, Number> point,
                                     String bidder,
                                     String time,
                                     double amount,
                                     boolean latestPoint) {
        point.nodeProperty().addListener((obs, oldNode, newNode) -> applyPointStyling(newNode, bidder, time, amount, latestPoint));
        if (point.getNode() != null) {
            applyPointStyling(point.getNode(), bidder, time, amount, latestPoint);
        }
    }

    private void applyPointStyling(Node node,
                                   String bidder,
                                   String time,
                                   double amount,
                                   boolean latestPoint) {
        if (node == null) {
            return;
        }

        if (!node.getStyleClass().contains("bid-history-point")) {
            node.getStyleClass().add("bid-history-point");
        }
        if (latestPoint && !node.getStyleClass().contains("bid-history-point-latest")) {
            node.getStyleClass().add("bid-history-point-latest");
        }

        node.setPickOnBounds(false);
        node.setOnMousePressed(event -> event.consume());
        node.setOnMouseReleased(event -> event.consume());
        node.setOnMouseDragged(event -> event.consume());

        Tooltip existingTooltip = (Tooltip) node.getProperties().get("bid-history-tooltip");
        if (existingTooltip != null) {
            return;
        }

        Label amountLabel = new Label(formatMoney(amount));
        amountLabel.getStyleClass().add("bid-history-tooltip-amount");

        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("bid-history-tooltip-time");

        VBox content = new VBox(amountLabel, timeLabel);
        content.getStyleClass().add("bid-history-tooltip-content");
        content.setMouseTransparent(true);

        Tooltip tooltip = new Tooltip();
        tooltip.setGraphic(content);
        tooltip.setText(null);
        tooltip.setShowDelay(Duration.millis(80));
        tooltip.setHideDelay(Duration.millis(60));
        tooltip.setShowDuration(Duration.seconds(12));
        tooltip.getStyleClass().add("bid-history-tooltip");
        tooltip.setConsumeAutoHidingEvents(true);
        node.getProperties().put("bid-history-tooltip", tooltip);
        Tooltip.install(node, tooltip);
    }

    private void styleSeries(XYChart.Series<String, Number> series) {
        if (series == null || series.getNode() == null) {
            return;
        }

        if (!series.getNode().getStyleClass().contains("bid-history-series")) {
            series.getNode().getStyleClass().add("bid-history-series");
        }
    }

    private void updateChartWidth(int pointCount) {
        if (bidLineChart == null) {
            return;
        }

        int safeCount = Math.max(pointCount, VISIBLE_TIME_SLOTS);
        double viewportWidth = CHART_EDGE_PADDING + (VISIBLE_TIME_SLOTS * TIME_SLOT_WIDTH);
        double chartWidth = CHART_EDGE_PADDING + (safeCount * TIME_SLOT_WIDTH);

        if (chartScrollPane != null) {
            chartScrollPane.setPrefViewportWidth(viewportWidth);
            chartScrollPane.setMinViewportWidth(viewportWidth);
        }

        bidLineChart.setMinWidth(chartWidth);
        bidLineChart.setPrefWidth(chartWidth);
        bidLineChart.setMaxWidth(chartWidth);
    }

    public record BidHistoryRow(String time, String bidder, String amount) {
    }
}
