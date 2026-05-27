package com.auction.client.controller;

import com.auction.client.dto.request.CreateAuctionRequest;
import com.auction.client.dto.response.AdminStatsResponse;
import com.auction.client.dto.response.AdminWalletActivityResponse;
import com.auction.client.dto.response.AuctionListResponse;
import com.auction.client.dto.response.AuctionPageResponse;
import com.auction.client.model.AdminApprovalItem;
import com.auction.client.navigation.SceneManager;
import com.auction.client.service.AdminApiService;
import com.auction.client.service.AuctionApiService;
import com.auction.client.session.SessionManager;
import com.auction.client.util.AuctionStateViewHelper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class AdminDashboardController {

    @FXML private Label totalAuctionsLabel;
    @FXML private Label activeSellersLabel;
    @FXML private Label revenueLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label activeAuctionsLabel;
    @FXML private Label closedAuctionsLabel;
    @FXML private Label newSellersLabel;
    @FXML private Label successRateLabel;
    @FXML private Label adminMessageLabel;
    @FXML private Button overviewTabButton;
    @FXML private Button auctionManagementTabButton;
    @FXML private ScrollPane adminScrollPane;
    @FXML private VBox overviewPane;
    @FXML private VBox adminPageBox;
    @FXML private VBox auctionManagementPane;
    @FXML private VBox walletActivityFeedBox;
    @FXML private VBox auctionRowsBox;
    @FXML private VBox reviewActionPanel;
    @FXML private Label reviewTitleLabel;
    @FXML private Label reviewMetaLabel;
    @FXML private Button reviewDeleteButton;
    @FXML private Button reviewAcceptButton;
    @FXML private Button reviewRejectButton;
    @FXML private Button reviewViewButton;

    private final AdminApiService adminApiService = new AdminApiService();
    private final AuctionApiService auctionApiService = new AuctionApiService();
    private final ObservableList<AdminApprovalItem> approvalItems = FXCollections.observableArrayList();
    private final ObservableList<AdminWalletActivityResponse> walletActivity = FXCollections.observableArrayList();
    private final ObservableList<AuctionListResponse> managedAuctions = FXCollections.observableArrayList();
    private final DateTimeFormatter sellerDateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter sellerDateTimeMinuteFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DateTimeFormatter isoMinuteFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private final DateTimeFormatter walletDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    private final ZoneId appZone = ZoneId.systemDefault();
    private AuctionListResponse selectedReviewAuction;
    private AdminApprovalItem selectedReviewPendingItem;

    @FXML
    public void initialize() {
        adminMessageLabel.setWrapText(true);
        adminMessageLabel.setMaxWidth(Double.MAX_VALUE);
        hideMessage();
        showOverviewTab();
        loadPendingItems();
        loadManagedAuctions();
        loadAdminStats();
        loadWalletActivity();
        clearReviewPanel();
    }

    private void loadPendingItems() {
        try {
            approvalItems.setAll(adminApiService.getPendingItems());
            hideMessage();
        } catch (Exception e) {
            approvalItems.clear();
            showMessage("Cannot load pending items: " + extractFriendlyMessage(e.getMessage()));
        }
    }

    private void loadAdminStats() {
        try {
            AdminStatsResponse stats = adminApiService.getStats();
            totalUsersLabel.setText(String.valueOf(stats.getTotalUsers()));
            activeSellersLabel.setText(String.valueOf(stats.getActiveSellers()));
            totalAuctionsLabel.setText(String.valueOf(stats.getTotalAuctions()));
            activeAuctionsLabel.setText(String.valueOf(stats.getActiveAuctions()));
            closedAuctionsLabel.setText(String.valueOf(stats.getClosedAuctions()));
            newSellersLabel.setText(String.valueOf(stats.getNewSellersThisMonth()));
            successRateLabel.setText(String.format("%.0f%%", stats.getAuctionSuccessRate()));
            revenueLabel.setText(formatCompactRevenue(stats.getRevenue()));
        } catch (Exception e) {
            populateAdminStatsFallback();
        }
    }

    private void loadWalletActivity() {
        try {
            walletActivity.setAll(adminApiService.getRecentWalletActivity(5));
            renderWalletActivityFeed();
        } catch (Exception e) {
            walletActivity.clear();
            renderWalletActivityFeed();
        }
    }

    private void approvePendingItem(AdminApprovalItem selectedItem) {
        if (!hasItemId(selectedItem, "approve")) {
            return;
        }

        try {
            adminApiService.approveAndCreateAuction(
                    selectedItem.getItemId(),
                    buildCreateAuctionRequest(selectedItem)
            );
            showSuccess("Item approved and auction created successfully.");
            reloadDashboardData();
        } catch (Exception e) {
            showMessage("Approve failed: " + extractFriendlyMessage(e.getMessage()));
        }
    }

    private void rejectPendingItem(AdminApprovalItem selectedItem) {
        if (!hasItemId(selectedItem, "reject")) {
            return;
        }

        try {
            adminApiService.rejectItem(selectedItem.getItemId());
            showSuccess("Item rejected.");
            reloadDashboardData();
        } catch (Exception e) {
            showMessage("Reject failed: " + extractFriendlyMessage(e.getMessage()));
        }
    }

    private void reviewPendingItem(AdminApprovalItem selectedItem) {
        if (selectedItem == null) {
            showMessage("Please select an item to review.");
            return;
        }

        selectedReviewPendingItem = selectedItem;
        selectedReviewAuction = findAuctionByItemId(selectedItem.getId());
        populateReviewPanel(
                firstNonBlank(selectedItem.getProductName(), "Untitled listing"),
                buildPendingReviewMeta(selectedItem)
        );
        showSuccess("Review panel ready.");
    }

    @FXML
    private void handleShowOverview() {
        showOverviewTab();
    }

    @FXML
    private void handleShowAuctionManagement() {
        loadPendingItems();
        loadManagedAuctions();
        showAuctionManagementTab();
    }

    private void deletePendingItem(AdminApprovalItem selectedItem) {
        if (!hasItemId(selectedItem, "delete")) {
            return;
        }

        try {
            adminApiService.deleteItem(selectedItem.getItemId());
            showSuccess("Item deleted.");
            reloadDashboardData();
        } catch (Exception e) {
            showMessage("Delete failed: " + extractFriendlyMessage(e.getMessage()));
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        SceneManager.goToAuth();
    }

    @FXML
    private void handleReviewDelete() {
        if (selectedReviewAuction != null && selectedReviewAuction.getId() != null) {
            try {
                adminApiService.deleteAuction(selectedReviewAuction.getId().toString());
                showSuccess("Auction marked as deleted.");
                reloadDashboardData();
            } catch (Exception e) {
                showMessage("Delete failed: " + extractFriendlyMessage(e.getMessage()));
            }
            return;
        }

        if (selectedReviewPendingItem != null) {
            deletePendingItem(selectedReviewPendingItem);
            return;
        }

        showMessage("Please review an auction first.");
    }

    @FXML
    private void handleReviewAccept() {
        if (selectedReviewAuction != null && selectedReviewAuction.getId() != null) {
            try {
                adminApiService.acceptAuction(selectedReviewAuction.getId().toString());
                showSuccess("Auction activated.");
                reloadDashboardData();
            } catch (Exception e) {
                showMessage("Accept failed: " + extractFriendlyMessage(e.getMessage()));
            }
            return;
        }

        if (selectedReviewPendingItem != null) {
            approvePendingItem(selectedReviewPendingItem);
            return;
        }

        showMessage("Please review an auction first.");
    }

    @FXML
    private void handleReviewReject() {
        if (selectedReviewAuction != null && selectedReviewAuction.getId() != null) {
            try {
                adminApiService.rejectAuction(selectedReviewAuction.getId().toString());
                showSuccess("Auction marked as rejected.");
                reloadDashboardData();
            } catch (Exception e) {
                showMessage("Reject failed: " + extractFriendlyMessage(e.getMessage()));
            }
            return;
        }

        if (selectedReviewPendingItem != null) {
            rejectPendingItem(selectedReviewPendingItem);
            return;
        }

        showMessage("Please review an auction first.");
    }

    @FXML
    private void handleReviewView() {
        if (selectedReviewAuction != null) {
            showSuccess("Viewing " + firstNonBlank(selectedReviewAuction.getTitle(), selectedReviewAuction.getItemName(), "auction")
                    + " | " + formatAuctionState(selectedReviewAuction)
                    + " | Current bid " + formatCurrency(selectedReviewAuction.getCurrentPrice())
                    + " | " + formatAdminDate(selectedReviewAuction.getStartTime()) + " -> " + formatAdminDate(selectedReviewAuction.getEndTime()));
            return;
        }

        if (selectedReviewPendingItem != null) {
            showSuccess("Viewing " + firstNonBlank(selectedReviewPendingItem.getProductName(), "listing")
                    + " | " + selectedReviewPendingItem.getStartingPriceText()
                    + " | " + firstNonBlank(selectedReviewPendingItem.getDescription(), "No description"));
            return;
        }

        showMessage("Please review an item first.");
    }

    private boolean hasItemId(AdminApprovalItem item, String action) {
        if (item == null) {
            showMessage("Please select an item to " + action + ".");
            return false;
        }

        if (item.getItemId() == null || item.getItemId().isBlank()) {
            showMessage("Selected item does not have itemId.");
            return false;
        }

        return true;
    }

    private void reloadDashboardData() {
        clearReviewPanel();
        loadPendingItems();
        loadManagedAuctions();
        loadAdminStats();
        loadWalletActivity();
    }

    private void showOverviewTab() {
        overviewPane.setManaged(true);
        overviewPane.setVisible(true);
        auctionManagementPane.setManaged(false);
        auctionManagementPane.setVisible(false);

        overviewTabButton.getStyleClass().remove("admin-tab-button-active");
        auctionManagementTabButton.getStyleClass().remove("admin-tab-button-active");
        if (!overviewTabButton.getStyleClass().contains("admin-tab-button-active")) {
            overviewTabButton.getStyleClass().add("admin-tab-button-active");
        }
    }

    private void showAuctionManagementTab() {
        overviewPane.setManaged(false);
        overviewPane.setVisible(false);
        auctionManagementPane.setManaged(true);
        auctionManagementPane.setVisible(true);

        overviewTabButton.getStyleClass().remove("admin-tab-button-active");
        auctionManagementTabButton.getStyleClass().remove("admin-tab-button-active");
        if (!auctionManagementTabButton.getStyleClass().contains("admin-tab-button-active")) {
            auctionManagementTabButton.getStyleClass().add("admin-tab-button-active");
        }
    }

    private CreateAuctionRequest buildCreateAuctionRequest(AdminApprovalItem item) {
        String startTime = toInstantTextOrDefaultStart(item.getStartDate());
        String endTime = toInstantTextOrDefaultEnd(item.getEndDate());
        Double startingPrice = item.getStartingPrice();
        Double reservePrice = item.getReservePrice();

        if (startingPrice == null || startingPrice <= 0) {
            startingPrice = 1.0;
        }
        if (reservePrice == null || reservePrice < 0) {
            reservePrice = 0.0;
        }

        return new CreateAuctionRequest(startTime, endTime, startingPrice, reservePrice);
    }

    private String toInstantTextOrDefaultStart(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now().plus(1, ChronoUnit.MINUTES).toString();
        }

        return parseSellerDateTime(value, true).toString();
    }

    private String toInstantTextOrDefaultEnd(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now().plus(7, ChronoUnit.DAYS).toString();
        }

        return parseSellerDateTime(value, false).toString();
    }

    private Instant parseSellerDateTime(String value, boolean startField) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            return startField
                    ? Instant.now().plus(1, ChronoUnit.MINUTES)
                    : Instant.now().plus(7, ChronoUnit.DAYS);
        }

        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDateTime localDateTime = LocalDateTime.parse(trimmed);
            return localDateTime.atZone(appZone).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDateTime localDateTime = LocalDateTime.parse(trimmed, sellerDateTimeFormat);
            return localDateTime.atZone(appZone).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDateTime localDateTime = LocalDateTime.parse(trimmed, sellerDateTimeMinuteFormat);
            return localDateTime.atZone(appZone).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDateTime localDateTime = LocalDateTime.parse(trimmed, isoMinuteFormat);
            return localDateTime.atZone(appZone).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDate localDate = LocalDate.parse(trimmed);
            LocalDateTime localDateTime = startField
                    ? localDate.atTime(0, 0, 0)
                    : localDate.atTime(23, 59, 59);
            return localDateTime.atZone(appZone).toInstant();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid seller schedule: " + value, ex);
        }
    }

    private String extractFriendlyMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "Unknown error.";
        }

        String extractedJsonMessage = extractJsonMessage(rawMessage);
        if (extractedJsonMessage != null && !extractedJsonMessage.isBlank()) {
            return extractedJsonMessage;
        }

        return rawMessage;
    }

    private String extractJsonMessage(String rawMessage) {
        int idx = rawMessage.indexOf("\"message\":\"");
        if (idx < 0) {
            return null;
        }

        int start = idx + 11;
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;

        for (int i = start; i < rawMessage.length(); i++) {
            char current = rawMessage.charAt(i);

            if (escaped) {
                switch (current) {
                    case '"' -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    default -> builder.append(current);
                }
                escaped = false;
                continue;
            }

            if (current == '\\') {
                escaped = true;
                continue;
            }

            if (current == '"') {
                return builder.toString();
            }

            builder.append(current);
        }

        return null;
    }

    private void showMessage(String message) {
        adminMessageLabel.setText(message);
        adminMessageLabel.setStyle("-fx-text-fill: #dc2626;");
        adminMessageLabel.setManaged(true);
        adminMessageLabel.setVisible(true);
    }

    private void showSuccess(String message) {
        adminMessageLabel.setText(message);
        adminMessageLabel.setStyle("-fx-text-fill: #16a34a;");
        adminMessageLabel.setManaged(true);
        adminMessageLabel.setVisible(true);
    }

    private void hideMessage() {
        adminMessageLabel.setText("");
        adminMessageLabel.setManaged(false);
        adminMessageLabel.setVisible(false);
    }

    private String formatCurrency(BigDecimal amount) {
        return amount == null ? "-" : formatCurrency(amount.doubleValue());
    }

    private String formatCurrency(double amount) {
        return "USD " + String.format(Locale.US, "%,.2f", amount);
    }

    private String formatWalletType(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        return value.replace('_', ' ');
    }

    private String formatWalletCreatedAt(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        try {
            return walletDateFormat.format(Instant.parse(value));
        } catch (Exception ignored) {
            return value;
        }
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void loadManagedAuctions() {
        try {
            AuctionPageResponse response = auctionApiService.searchAuctions(null, null, null, 0, 20, "createdAt,desc");
            managedAuctions.setAll((response.getItems() == null ? java.util.List.<AuctionListResponse>of() : response.getItems())
                    .stream()
                    .sorted(Comparator
                            .comparingInt(this::auctionPriority)
                            .thenComparing(AuctionListResponse::getStartTime, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(AuctionListResponse::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .toList());
            renderManagedAuctions();
        } catch (Exception e) {
            managedAuctions.clear();
            renderManagedAuctions();
            showMessage("Cannot load auctions: " + extractFriendlyMessage(e.getMessage()));
        }
    }

    private void renderWalletActivityFeed() {
        if (walletActivityFeedBox == null) {
            return;
        }

        walletActivityFeedBox.getChildren().clear();
        if (walletActivity.isEmpty()) {
            walletActivityFeedBox.getChildren().add(buildEmptyState("No recent wallet activity."));
            return;
        }

        for (AdminWalletActivityResponse activity : walletActivity) {
            walletActivityFeedBox.getChildren().add(buildWalletActivityCard(activity));
        }
    }

    private Node buildWalletActivityCard(AdminWalletActivityResponse activity) {
        String type = activity == null ? "" : firstNonBlank(activity.getType(), "");
        String cardClass = switch (type.toUpperCase(Locale.ROOT)) {
            case "DEPOSIT" -> "admin-note-card-deposit";
            case "WITHDRAW", "WITHDRAWAL" -> "admin-note-card-withdrawal";
            default -> "admin-note-card-default";
        };

        Label title = new Label(firstNonBlank(activity == null ? null : activity.getUserDisplayName(), "Unknown user"));
        title.getStyleClass().add("admin-note-title");

        Label body = new Label((activity == null ? "-" : formatCurrency(activity.getAmount()))
                + " | Balance " + (activity == null ? "-" : formatCurrency(activity.getBalanceAfter())));
        body.getStyleClass().add("admin-note-body");

        Label meta = new Label(activity == null ? "-" : formatWalletCreatedAt(activity.getCreatedAt()));
        meta.getStyleClass().add("admin-note-meta");

        VBox copy = new VBox(4, title, body, meta);
        HBox.setHgrow(copy, Priority.ALWAYS);

        Label tag = new Label(type.isBlank() ? "Activity" : formatWalletType(type));
        tag.getStyleClass().add("admin-note-tag");

        HBox row = new HBox(12, copy, tag);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("admin-note-card", cardClass);
        return row;
    }

    private void renderManagedAuctions() {
        if (auctionRowsBox == null) {
            return;
        }

        auctionRowsBox.getChildren().clear();
        for (AdminApprovalItem item : approvalItems.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(AdminApprovalItem::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList()) {
            auctionRowsBox.getChildren().add(buildPendingRow(item));
        }

        for (AuctionListResponse auction : managedAuctions) {
            auctionRowsBox.getChildren().add(buildAuctionRow(auction));
        }

        if (auctionRowsBox.getChildren().isEmpty()) {
            auctionRowsBox.getChildren().add(buildEmptyState("No auctions found."));
        }
    }

    private Node buildAuctionRow(AuctionListResponse auction) {
        Label title = new Label(firstNonBlank(auction.getTitle(), auction.getItemName(), "Untitled auction"));
        title.getStyleClass().add("admin-management-title");
        title.setWrapText(true);

        Label subtitle = new Label("Category " + firstNonBlank(auction.getCategory(), "General")
                + " | Start " + formatAdminDate(auction.getStartTime())
                + " | End " + formatAdminDate(auction.getEndTime()));
        subtitle.getStyleClass().add("admin-management-subtitle");
        subtitle.setWrapText(true);

        VBox titleBox = new VBox(4, title, subtitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label seller = new Label(firstNonBlank(auction.getSellerName(), "Unknown seller"));
        seller.getStyleClass().add("admin-management-title");

        Label sellerMeta = new Label("Seller ID " + shortUuid(auction.getSellerId())
                + " | Item ID " + shortUuid(auction.getItemId()));
        sellerMeta.getStyleClass().add("admin-management-subtitle");
        sellerMeta.setWrapText(true);

        VBox sellerBox = new VBox(4, seller, sellerMeta);
        sellerBox.setMinWidth(210);

        Label status = new Label(formatAuctionState(auction));
        status.getStyleClass().addAll("admin-status-pill", adminStateClass(auction));
        status.setMinWidth(120);

        Label currentBid = new Label(formatCurrency(BigDecimal.valueOf(auction.getCurrentPrice())));
        currentBid.getStyleClass().add("admin-management-title");

        Label currentBidMeta = new Label("Bids " + auction.getBidCount()
                + " | Min next " + formatCurrency(auction.getMinNextBid()));
        currentBidMeta.getStyleClass().add("admin-management-subtitle");
        currentBidMeta.setWrapText(true);

        VBox priceBox = new VBox(4, currentBid, currentBidMeta);
        priceBox.setMinWidth(170);

        Button review = new Button("Review");
        review.getStyleClass().add("admin-inline-link");
        review.setOnAction(event -> reviewAuctionRow(auction));
        review.setMinWidth(110);

        HBox row = new HBox(18, titleBox, sellerBox, status, priceBox, review);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("admin-management-row", adminRowClass(auction));
        return row;
    }

    private Node buildPendingRow(AdminApprovalItem item) {
        Label title = new Label(firstNonBlank(item == null ? null : item.getProductName(), "Untitled pending item"));
        title.getStyleClass().add("admin-management-title");
        title.setWrapText(true);

        Label subtitle = new Label("Category " + firstNonBlank(item == null ? null : item.getCategory(), "General")
                + " | Start " + formatAdminDate(item == null ? null : item.getStartDate())
                + " | End " + formatAdminDate(item == null ? null : item.getEndDate()));
        subtitle.getStyleClass().add("admin-management-subtitle");
        subtitle.setWrapText(true);

        VBox titleBox = new VBox(4, title, subtitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label seller = new Label(firstNonBlank(item == null ? null : item.getSellerName(), "Unknown seller"));
        seller.getStyleClass().add("admin-management-title");

        Label sellerMeta = new Label("Seller ID " + shortUuid(item == null ? null : item.getSellerId())
                + " | Item ID " + shortUuid(item == null ? null : item.getId()));
        sellerMeta.getStyleClass().add("admin-management-subtitle");
        sellerMeta.setWrapText(true);

        VBox sellerBox = new VBox(4, seller, sellerMeta);
        sellerBox.setMinWidth(210);

        Label status = new Label(formatPendingState(item));
        status.getStyleClass().addAll("admin-status-pill", adminStateClass(item));
        status.setMinWidth(120);

        Label currentBid = new Label(item == null ? "-" : item.getStartingPriceText());
        currentBid.getStyleClass().add("admin-management-title");

        String reserveText = item == null || item.getReservePrice() == null
                ? "-"
                : formatCurrency(item.getReservePrice());
        Label currentBidMeta = new Label("Reserve " + reserveText);
        currentBidMeta.getStyleClass().add("admin-management-subtitle");

        VBox priceBox = new VBox(4, currentBid, currentBidMeta);
        priceBox.setMinWidth(170);

        Button review = new Button("Review");
        review.getStyleClass().add("admin-inline-link");
        review.setOnAction(event -> reviewPendingItem(item));
        review.setMinWidth(110);

        HBox row = new HBox(18, titleBox, sellerBox, status, priceBox, review);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("admin-management-row", adminRowClass(item));
        return row;
    }

    private Node buildEmptyState(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("admin-muted");
        VBox box = new VBox(label);
        box.getStyleClass().add("admin-pending-card");
        return box;
    }

    private void reviewAuction(AuctionListResponse auction) {
        if (auction == null) {
            showMessage("Auction is unavailable.");
            return;
        }

        showSuccess("Reviewing " + firstNonBlank(auction.getTitle(), auction.getItemName(), "auction")
                + " | " + formatAuctionState(auction)
                + " | " + formatAdminDate(auction.getStartTime()) + " -> " + formatAdminDate(auction.getEndTime()));
    }

    private void reviewAuctionRow(AuctionListResponse auction) {
        if (auction == null) {
            showMessage("Auction is unavailable.");
            return;
        }

        selectedReviewAuction = auction;
        selectedReviewPendingItem = null;
        populateReviewPanel(
                firstNonBlank(auction.getTitle(), auction.getItemName(), "Untitled auction"),
                buildAuctionReviewMeta(auction)
        );
        reviewAuction(auction);
    }

    private String formatAdminDate(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        try {
            return walletDateFormat.format(Instant.parse(value));
        } catch (Exception ignored) {
            return value;
        }
    }

    private String adminRowClass(AuctionListResponse auction) {
        return switch (normalizeAuctionState(auction)) {
            case "pending" -> "admin-management-row-pending";
            case "active" -> "admin-management-row-active";
            case "scheduled" -> "admin-management-row-incoming";
            case "rejected" -> "admin-management-row-rejected";
            case "deleted" -> "admin-management-row-deleted";
            default -> "admin-management-row-closed";
        };
    }

    private String adminRowClass(AdminApprovalItem item) {
        return switch (normalizePendingState(item)) {
            case "pending" -> "admin-management-row-pending";
            case "active" -> "admin-management-row-active";
            case "scheduled" -> "admin-management-row-incoming";
            case "rejected" -> "admin-management-row-rejected";
            case "deleted" -> "admin-management-row-deleted";
            default -> "admin-management-row-closed";
        };
    }

    private String adminStateClass(AuctionListResponse auction) {
        return switch (normalizeAuctionState(auction)) {
            case "pending" -> "admin-status-pending";
            case "active" -> "admin-status-active";
            case "scheduled" -> "admin-status-incoming";
            case "rejected" -> "admin-status-rejected";
            case "deleted" -> "admin-status-deleted";
            default -> "admin-status-closed";
        };
    }

    private String adminStateClass(AdminApprovalItem item) {
        return switch (normalizePendingState(item)) {
            case "pending" -> "admin-status-pending";
            case "active" -> "admin-status-active";
            case "scheduled" -> "admin-status-incoming";
            case "rejected" -> "admin-status-rejected";
            case "deleted" -> "admin-status-deleted";
            default -> "admin-status-closed";
        };
    }

    private String formatAuctionState(AuctionListResponse auction) {
        return normalizeAuctionState(auction);
    }

    private String formatPendingState(AdminApprovalItem item) {
        return normalizePendingState(item);
    }

    private String normalizeAuctionState(AuctionListResponse auction) {
        if (auction == null) {
            return "closed";
        }

        return toAdminState(AuctionStateViewHelper.resolveDisplayState(
                auction.getState(),
                auction.getStartTime(),
                auction.getEndTime()
        ));
    }

    private String normalizePendingState(AdminApprovalItem item) {
        if (item == null) {
            return "pending";
        }

        String state = item.getStatus();
        return state == null || state.isBlank() ? "pending" : toAdminState(state);
    }

    private String toAdminState(String state) {
        if (state == null || state.isBlank()) {
            return "scheduled";
        }

        return switch (state.trim().toUpperCase(Locale.ROOT)) {
            case "PENDING" -> "pending";
            case "ACTIVE" -> "active";
            case "SCHEDULED", "DRAFT", "INCOMING" -> "scheduled";
            case "FINISHED", "CANCELLED", "CLOSED", "ENDED" -> "closed";
            case "REJECTED" -> "rejected";
            case "DELETED" -> "deleted";
            default -> "closed";
        };
    }

    private int auctionPriority(AuctionListResponse auction) {
        if (auction == null) {
            return Integer.MAX_VALUE;
        }

        String normalized = normalizeAuctionState(auction);
        return switch (normalized) {
            case "scheduled" -> 0;
            case "active" -> 1;
            case "closed" -> 2;
            case "deleted" -> 3;
            case "rejected" -> 4;
            default -> 5;
        };
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

    private void populateReviewPanel(String title, String meta) {
        if (reviewActionPanel == null) {
            return;
        }

        reviewActionPanel.setManaged(true);
        reviewActionPanel.setVisible(true);
        reviewTitleLabel.setText(firstNonBlank(title, "No item selected"));
        reviewMetaLabel.setText(firstNonBlank(meta, "No review details."));
        refreshReviewButtons();
        scrollToReviewPanel();
    }

    private void refreshReviewButtons() {
        boolean hasAnySelection = selectedReviewAuction != null || selectedReviewPendingItem != null;

        if (reviewDeleteButton != null) {
            reviewDeleteButton.setDisable(!hasAnySelection);
        }
        if (reviewAcceptButton != null) {
            reviewAcceptButton.setDisable(!hasAnySelection);
        }
        if (reviewRejectButton != null) {
            reviewRejectButton.setDisable(!hasAnySelection);
        }
        if (reviewViewButton != null) {
            reviewViewButton.setDisable(!hasAnySelection);
        }
    }

    private void clearReviewPanel() {
        selectedReviewAuction = null;
        selectedReviewPendingItem = null;

        if (reviewActionPanel != null) {
            reviewActionPanel.setManaged(false);
            reviewActionPanel.setVisible(false);
        }
        if (reviewTitleLabel != null) {
            reviewTitleLabel.setText("No item selected");
        }
        if (reviewMetaLabel != null) {
            reviewMetaLabel.setText("Choose an auction to review.");
        }
        refreshReviewButtons();
    }

    private String buildAuctionReviewMeta(AuctionListResponse auction) {
        StringBuilder builder = new StringBuilder();
        builder.append("Seller ").append(firstNonBlank(auction.getSellerName(), "Unknown seller"));
        builder.append(" | Status ").append(formatAuctionState(auction));
        builder.append(" | Current bid ").append(formatCurrency(auction.getCurrentPrice()));
        builder.append(" | Start ").append(formatAdminDate(auction.getStartTime()));
        builder.append(" | End ").append(formatAdminDate(auction.getEndTime()));
        builder.append(" | Accept=activate auction, Reject=mark rejected, Delete=mark deleted");
        return builder.toString();
    }

    private String buildPendingReviewMeta(AdminApprovalItem item) {
        return "Seller " + firstNonBlank(item.getSellerName(), "Unknown seller")
                + " | Status pending"
                + " | Category " + firstNonBlank(item.getCategory(), "General")
                + " | Price " + item.getStartingPriceText()
                + " | Start " + firstNonBlank(formatAdminDate(item.getStartDate()), "-")
                + " | End " + firstNonBlank(formatAdminDate(item.getEndDate()), "-");
    }

    private AdminApprovalItem findPendingItemByItemId(UUID itemId) {
        if (itemId == null) {
            return null;
        }

        return approvalItems.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null && itemId.equals(item.getId()))
                .findFirst()
                .orElse(null);
    }

    private AuctionListResponse findAuctionByItemId(UUID itemId) {
        if (itemId == null) {
            return null;
        }

        return managedAuctions.stream()
                .filter(Objects::nonNull)
                .filter(auction -> itemId.equals(auction.getItemId()))
                .min(Comparator.comparing(AuctionListResponse::getStartTime, Comparator.nullsLast(String::compareTo)))
                .orElse(null);
    }

    private void scrollToReviewPanel() {
        if (adminScrollPane == null || reviewActionPanel == null || adminPageBox == null) {
            return;
        }

        Platform.runLater(() -> {
            double contentHeight = adminPageBox.getBoundsInLocal().getHeight();
            double viewportHeight = adminScrollPane.getViewportBounds().getHeight();
            double panelY = reviewActionPanel.getBoundsInParent().getMinY();

            if (contentHeight <= viewportHeight) {
                adminScrollPane.setVvalue(0);
                return;
            }

            double target = panelY / Math.max(1, contentHeight - viewportHeight);
            adminScrollPane.setVvalue(Math.max(0, Math.min(target, 1)));
        });
    }

    private void populateAdminStatsFallback() {
        long totalAuctions = managedAuctions.size();
        long activeAuctions = managedAuctions.stream()
                .filter(auction -> "active".equals(normalizeAuctionState(auction)))
                .count();
        long closedAuctions = managedAuctions.stream()
                .filter(auction -> "closed".equals(normalizeAuctionState(auction)))
                .count();
        long activeSellers = managedAuctions.stream()
                .map(AuctionListResponse::getSellerId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        long successfulAuctions = managedAuctions.stream()
                .filter(auction -> "closed".equals(normalizeAuctionState(auction)))
                .filter(auction -> auction.getWinnerId() != null || auction.getLeaderId() != null)
                .count();
        double revenue = managedAuctions.stream()
                .filter(auction -> "closed".equals(normalizeAuctionState(auction)))
                .filter(auction -> auction.getWinnerId() != null || auction.getLeaderId() != null)
                .mapToDouble(AuctionListResponse::getCurrentPrice)
                .sum();

        totalUsersLabel.setText("-");
        activeSellersLabel.setText(String.valueOf(activeSellers));
        totalAuctionsLabel.setText(String.valueOf(totalAuctions));
        activeAuctionsLabel.setText(String.valueOf(activeAuctions));
        closedAuctionsLabel.setText(String.valueOf(closedAuctions));
        newSellersLabel.setText("-");
        successRateLabel.setText(closedAuctions == 0
                ? "0%"
                : String.format("%.0f%%", (successfulAuctions * 100.0) / closedAuctions));
        revenueLabel.setText(formatCompactRevenue(revenue));
    }

    private String formatCompactRevenue(double revenue) {
        if (Math.abs(revenue) >= 1000) {
            return "USD " + String.format("%,.1fk", revenue / 1000.0);
        }
        return "USD " + String.format("%,.0f", revenue);
    }

    private String shortUuid(UUID value) {
        if (value == null) {
            return "-";
        }

        String text = value.toString();
        return text.length() <= 8 ? text : text.substring(0, 8);
    }
}
