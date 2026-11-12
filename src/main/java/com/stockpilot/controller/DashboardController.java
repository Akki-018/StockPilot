package com.stockpilot.controller;

import com.stockpilot.model.ActivityItem;
import com.stockpilot.model.DashboardSummary;
import com.stockpilot.service.DashboardService;
import com.stockpilot.util.ViewHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Builds the redesigned StockPilot Dashboard view following the
 * "Industrial Minimalism + Editorial Design" system.
 */
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController() {
        this.dashboardService = new DashboardService();
    }

    public Node createView() {
        VBox root = new VBox(22);
        root.getStyleClass().add("content-container");
        root.setPadding(new Insets(24, 28, 28, 28));

        HBox metricsStrip = buildMetricsStrip(dashboardService.getDashboardSummary());
        VBox recentActivitySection = buildRecentActivitySection();

        root.getChildren().addAll(metricsStrip, recentActivitySection);
        return root;
    }

    /**
     * Builds a unified, editorial metrics strip with restrained accent colors and clean typographic hierarchy.
     */
    private HBox buildMetricsStrip(DashboardSummary summary) {
        HBox strip = new HBox(0);
        strip.getStyleClass().add("metrics-strip");
        strip.setAlignment(Pos.CENTER_LEFT);

        VBox segProducts = ViewHelper.createStatSegment(
                "TOTAL PRODUCTS",
                String.format("%,d", summary.getTotalProducts()),
                "Active catalog SKUs",
                ""
        );

        VBox segUnits = ViewHelper.createStatSegment(
                "STOCK ON HAND",
                String.format("%,d", summary.getTotalStockUnits()),
                "Total units in inventory",
                ""
        );

        VBox segLowStock = ViewHelper.createStatSegment(
                "LOW STOCK BUFFER",
                String.valueOf(summary.getLowStockItems()),
                "Below reorder threshold",
                "metric-value-warning"
        );

        VBox segOutOfStock = ViewHelper.createStatSegment(
                "DEPLETED STOCK",
                String.valueOf(summary.getOutOfStockItems()),
                "Immediate restock required",
                "metric-value-danger"
        );

        strip.getChildren().addAll(
                segProducts,
                createMetricDivider(),
                segUnits,
                createMetricDivider(),
                segLowStock,
                createMetricDivider(),
                segOutOfStock
        );

        return strip;
    }

    private Region createMetricDivider() {
        Region divider = new Region();
        divider.getStyleClass().add("metric-divider");
        return divider;
    }

    /**
     * Builds the compact recent inventory activity section without unnecessary empty rows.
     */
    private VBox buildRecentActivitySection() {
        VBox sectionCard = new VBox(0);
        sectionCard.getStyleClass().add("section-card");

        // Section Header
        HBox header = new HBox(12);
        header.getStyleClass().add("section-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleGroup = new VBox(2);
        Label heading = new Label("RECENT INVENTORY MOVEMENTS");
        heading.getStyleClass().add("section-title");

        Label subheading = new Label("Real-time ledger events and stock threshold alerts");
        subheading.getStyleClass().add("section-subtitle");
        titleGroup.getChildren().addAll(heading, subheading);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label recordCountBadge = new Label("5 ENTRIES LOGGED");
        recordCountBadge.getStyleClass().add("meta-tag");

        Label liveIndicator = new Label("● LIVE LEDGER");
        liveIndicator.getStyleClass().add("live-status-pill");

        header.getChildren().addAll(titleGroup, spacer, recordCountBadge, liveIndicator);

        // Activity Table
        TableView<ActivityItem> activityTable = buildActivityTable();

        // Section Footer Bar
        HBox footer = new HBox(12);
        footer.getStyleClass().add("section-footer");
        footer.setAlignment(Pos.CENTER_LEFT);

        Label footerLeft = new Label("LEDGER STATUS: SYNCHRONIZED");
        footerLeft.getStyleClass().add("footer-label");

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Label footerRight = new Label("LOG STAGE: IN-MEMORY (DAO LAYER)");
        footerRight.getStyleClass().add("footer-label-secondary");

        footer.getChildren().addAll(footerLeft, footerSpacer, footerRight);

        sectionCard.getChildren().addAll(header, activityTable, footer);
        return sectionCard;
    }

    private TableView<ActivityItem> buildActivityTable() {
        TableView<ActivityItem> table = new TableView<>();
        table.getStyleClass().add("activity-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No recent inventory activity recorded."));

        // Fixed cell sizing to prevent ghost empty rows
        table.setFixedCellSize(44.0);

        TableColumn<ActivityItem, String> timeCol = new TableColumn<>("TIMESTAMP");
        timeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTimestamp()));
        timeCol.setMinWidth(140);
        timeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    getStyleClass().add("cell-timestamp");
                }
            }
        });

        TableColumn<ActivityItem, String> actionCol = new TableColumn<>("EVENT TYPE");
        actionCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getAction()));
        actionCol.setMinWidth(130);
        actionCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String action, boolean empty) {
                super.updateItem(action, empty);
                if (empty || action == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label tag = new Label(action.toUpperCase());
                tag.getStyleClass().add("action-tag");
                if (action.contains("Stock In")) {
                    tag.getStyleClass().add("action-tag-in");
                } else if (action.contains("Stock Out")) {
                    tag.getStyleClass().add("action-tag-out");
                } else {
                    tag.getStyleClass().add("action-tag-alert");
                }
                setGraphic(tag);
                setText(null);
            }
        });

        TableColumn<ActivityItem, String> itemCol = new TableColumn<>("PRODUCT / SKU DESCRIPTION");
        itemCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getItemName()));
        itemCol.setMinWidth(240);
        itemCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    getStyleClass().add("cell-item-name");
                }
            }
        });

        TableColumn<ActivityItem, String> changeCol = new TableColumn<>("STOCK DELTA");
        changeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getQuantityChange()));
        changeCol.setMinWidth(130);
        changeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String delta, boolean empty) {
                super.updateItem(delta, empty);
                if (empty || delta == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label deltaLabel = new Label(delta);
                deltaLabel.getStyleClass().add("delta-label");

                if (delta.startsWith("+")) {
                    deltaLabel.setText("▲ " + delta);
                    deltaLabel.getStyleClass().add("delta-in");
                } else if (delta.startsWith("-")) {
                    deltaLabel.setText("▼ " + delta);
                    deltaLabel.getStyleClass().add("delta-out");
                } else {
                    deltaLabel.setText("■ " + delta);
                    deltaLabel.getStyleClass().add("delta-alert");
                }

                setGraphic(deltaLabel);
                setText(null);
            }
        });

        TableColumn<ActivityItem, String> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus()));
        statusCol.setMinWidth(110);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label badge = new Label(status.toUpperCase());
                badge.getStyleClass().add("status-pill");
                switch (status) {
                    case "Completed" -> badge.getStyleClass().add("status-completed");
                    case "Attention" -> badge.getStyleClass().add("status-attention");
                    case "Critical" -> badge.getStyleClass().add("status-critical");
                    default -> badge.getStyleClass().add("status-neutral");
                }
                setGraphic(badge);
                setText(null);
            }
        });

        table.getColumns().add(timeCol);
        table.getColumns().add(actionCol);
        table.getColumns().add(itemCol);
        table.getColumns().add(changeCol);
        table.getColumns().add(statusCol);

        table.setItems(FXCollections.observableArrayList(dashboardService.getRecentActivity()));

        // Bind height to items + header (approx 38px) to eliminate blank rows
        table.prefHeightProperty().bind(
                table.fixedCellSizeProperty()
                        .multiply(Bindings.size(table.getItems()))
                        .add(38)
        );
        table.setMinHeight(Region.USE_PREF_SIZE);
        table.setMaxHeight(Region.USE_PREF_SIZE);

        return table;
    }
}
