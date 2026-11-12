package com.stockpilot.controller;

import com.stockpilot.model.ActivityItem;
import com.stockpilot.model.CategoryStock;
import com.stockpilot.model.DailyMovement;
import com.stockpilot.model.DashboardSummary;
import com.stockpilot.model.InventoryHealth;
import com.stockpilot.service.DashboardService;
import com.stockpilot.util.ViewHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Builds the redesigned StockPilot Dashboard view featuring:
 * 1. Compact Editorial Inventory Metrics
 * 2. 7-Day Stock Movement LineChart
 * 3. Inventory Health Segmented Visual Indicator
 * 4. Category Distribution PieChart
 * 5. Category Stock Levels BarChart
 * 6. Compact Recent Activity Table (No ghost rows)
 */
public class DashboardController {

    private final DashboardService dashboardService;
    private final java.text.NumberFormat currencyFormatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.US);

    public DashboardController() {
        this.dashboardService = new DashboardService();
    }

    public Node createView() {
        VBox contentBox = new VBox(20);
        contentBox.getStyleClass().add("content-container");
        contentBox.setPadding(new Insets(22, 26, 26, 26));

        // A. Inventory Metrics Ribbon
        HBox metricsStrip = buildMetricsStrip(dashboardService.getDashboardSummary());

        // Upper Analytics Row: Stock Movement LineChart (left) & Health + PieChart (right)
        HBox upperRow = new HBox(18);
        upperRow.setAlignment(Pos.TOP_LEFT);

        VBox movementCard = buildStockMovementCard(dashboardService.getStockMovements());
        VBox healthAndCategoryCard = buildHealthAndCategoryCard(
                dashboardService.getInventoryHealth(),
                dashboardService.getCategoryStocks()
        );

        HBox.setHgrow(movementCard, Priority.ALWAYS);
        HBox.setHgrow(healthAndCategoryCard, Priority.ALWAYS);
        upperRow.getChildren().addAll(movementCard, healthAndCategoryCard);

        // Lower Analytics Row: Category BarChart (left) & Compact Activity Table (right)
        HBox lowerRow = new HBox(18);
        lowerRow.setAlignment(Pos.TOP_LEFT);

        VBox categoryBarCard = buildCategoryStockLevelsCard(dashboardService.getCategoryStocks());
        VBox recentActivityCard = buildRecentActivityCard();

        HBox.setHgrow(categoryBarCard, Priority.ALWAYS);
        HBox.setHgrow(recentActivityCard, Priority.ALWAYS);
        lowerRow.getChildren().addAll(categoryBarCard, recentActivityCard);

        contentBox.getChildren().addAll(metricsStrip, upperRow, lowerRow);

        // Responsive ScrollPane wrapper so dashboard scales gracefully on all displays
        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("dashboard-scroll-pane");
        return scrollPane;
    }

    /**
     * A. Compact editorial inventory metrics strip.
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

        VBox segValuation = ViewHelper.createStatSegment(
                "INVENTORY VALUATION",
                currencyFormatter.format(summary.getInventoryValuation()),
                "Live asset valuation on hand",
                ""
        );

        strip.getChildren().addAll(
                segProducts,
                createMetricDivider(),
                segUnits,
                createMetricDivider(),
                segLowStock,
                createMetricDivider(),
                segOutOfStock,
                createMetricDivider(),
                segValuation
        );

        return strip;
    }

    private Region createMetricDivider() {
        Region divider = new Region();
        divider.getStyleClass().add("metric-divider");
        return divider;
    }

    /**
     * C. Stock Movement LineChart card showing 7-day velocity.
     */
    private VBox buildStockMovementCard(List<DailyMovement> movements) {
        VBox card = new VBox(10);
        card.getStyleClass().add("section-card");

        HBox header = buildCardHeader(
                "STOCK MOVEMENT // 7-DAY VELOCITY",
                "Daily inbound receipts vs outbound dispatches",
                "DEMO DATASET"
        );

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("TIMELINE");
        xAxis.setAnimated(false);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("UNITS");
        yAxis.setAnimated(false);
        yAxis.setAutoRanging(true);

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle(null);
        lineChart.setLegendVisible(true);
        lineChart.setAnimated(false);
        lineChart.setCreateSymbols(true);
        lineChart.setPrefHeight(260);
        lineChart.setMinHeight(240);

        XYChart.Series<String, Number> stockInSeries = new XYChart.Series<>();
        stockInSeries.setName("Stock In");

        XYChart.Series<String, Number> stockOutSeries = new XYChart.Series<>();
        stockOutSeries.setName("Stock Out");

        for (DailyMovement dm : movements) {
            stockInSeries.getData().add(new XYChart.Data<>(dm.getDateLabel(), dm.getStockInUnits()));
            stockOutSeries.getData().add(new XYChart.Data<>(dm.getDateLabel(), dm.getStockOutUnits()));
        }

        lineChart.getData().add(stockInSeries);
        lineChart.getData().add(stockOutSeries);

        card.getChildren().addAll(header, lineChart);
        return card;
    }

    /**
     * B & E. Card combining Inventory Health visual meter and Category Distribution PieChart.
     */
    private VBox buildHealthAndCategoryCard(InventoryHealth health, List<CategoryStock> categories) {
        VBox card = new VBox(12);
        card.getStyleClass().add("section-card");

        HBox header = buildCardHeader(
                "INVENTORY HEALTH & PORTFOLIO MIX",
                "Stock condition ratio and category share",
                "ANALYTICS"
        );

        // E. Inventory Health Visual Meter
        VBox healthWidget = buildHealthWidget(health);

        // B. Inventory Distribution PieChart
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (CategoryStock cat : categories) {
            pieData.add(new PieChart.Data(cat.getCategoryName(), cat.getProductCount()));
        }

        PieChart pieChart = new PieChart(pieData);
        pieChart.setTitle(null);
        pieChart.setLegendVisible(true);
        pieChart.setLabelsVisible(false);
        pieChart.setAnimated(false);
        pieChart.setPrefHeight(180);
        pieChart.setMinHeight(160);

        card.getChildren().addAll(header, healthWidget, pieChart);
        return card;
    }

    private VBox buildHealthWidget(InventoryHealth health) {
        VBox container = new VBox(6);
        container.getStyleClass().add("health-widget-container");
        container.setPadding(new Insets(4, 8, 4, 8));

        HBox topLabelRow = new HBox();
        topLabelRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("OVERALL STOCK CONDITION");
        title.getStyleClass().add("health-widget-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label percentageLabel = new Label(String.format("%.1f%% HEALTHY", health.getHealthyPercentage()));
        percentageLabel.getStyleClass().add("health-percentage-badge");

        topLabelRow.getChildren().addAll(title, spacer, percentageLabel);

        // Segmented proportional progress bar using GridPane column constraints
        GridPane bar = new GridPane();
        bar.getStyleClass().add("health-bar-track");
        bar.setHgap(3);

        ColumnConstraints colHealthy = new ColumnConstraints();
        colHealthy.setPercentWidth(health.getHealthyPercentage());

        ColumnConstraints colLow = new ColumnConstraints();
        colLow.setPercentWidth(health.getLowStockPercentage());

        ColumnConstraints colOut = new ColumnConstraints();
        colOut.setPercentWidth(health.getOutOfStockPercentage());

        bar.getColumnConstraints().addAll(colHealthy, colLow, colOut);

        Region segHealthy = new Region();
        segHealthy.getStyleClass().add("health-seg-healthy");
        segHealthy.setPrefHeight(7);

        Region segLow = new Region();
        segLow.getStyleClass().add("health-seg-low");
        segLow.setPrefHeight(7);

        Region segOut = new Region();
        segOut.getStyleClass().add("health-seg-out");
        segOut.setPrefHeight(7);

        bar.add(segHealthy, 0, 0);
        bar.add(segLow, 1, 0);
        bar.add(segOut, 2, 0);

        // Legend Breakdown Row
        HBox legendRow = new HBox(12);
        legendRow.setAlignment(Pos.CENTER_LEFT);
        legendRow.setPadding(new Insets(2, 0, 0, 0));

        Label healthyTag = new Label(String.format("● Healthy: %d (%.1f%%)",
                health.getHealthyItems(), health.getHealthyPercentage()));
        healthyTag.getStyleClass().add("health-legend-healthy");

        Label lowStockTag = new Label(String.format("● Low: %d (%.1f%%)",
                health.getLowStockItems(), health.getLowStockPercentage()));
        lowStockTag.getStyleClass().add("health-legend-low");

        Label outTag = new Label(String.format("● Depleted: %d (%.1f%%)",
                health.getOutOfStockItems(), health.getOutOfStockPercentage()));
        outTag.getStyleClass().add("health-legend-out");

        legendRow.getChildren().addAll(healthyTag, lowStockTag, outTag);

        container.getChildren().addAll(topLabelRow, bar, legendRow);
        return container;
    }

    /**
     * D. Category Stock Levels BarChart comparing quantities across categories.
     */
    private VBox buildCategoryStockLevelsCard(List<CategoryStock> categories) {
        VBox card = new VBox(10);
        card.getStyleClass().add("section-card");

        HBox header = buildCardHeader(
                "CATEGORY STOCK LEVELS",
                "Total units stocked across inventory divisions",
                "CAPACITY"
        );

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setAnimated(false);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("UNITS ON HAND");
        yAxis.setAnimated(false);
        yAxis.setAutoRanging(true);

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle(null);
        barChart.setLegendVisible(false);
        barChart.setAnimated(false);
        barChart.setPrefHeight(250);
        barChart.setMinHeight(230);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Units on Hand");

        for (CategoryStock cat : categories) {
            series.getData().add(new XYChart.Data<>(cat.getCategoryName(), cat.getUnitCount()));
        }

        barChart.getData().add(series);

        card.getChildren().addAll(header, barChart);
        return card;
    }

    /**
     * F. Compact recent inventory activity table without empty rows.
     */
    private VBox buildRecentActivityCard() {
        VBox card = new VBox(0);
        card.getStyleClass().add("section-card");

        HBox header = buildCardHeader(
                "RECENT INVENTORY MOVEMENTS",
                "Latest transactions and threshold alerts",
                "DEMO LEDGER"
        );

        TableView<ActivityItem> table = buildActivityTable();

        HBox footer = new HBox(12);
        footer.getStyleClass().add("section-footer");
        footer.setAlignment(Pos.CENTER_LEFT);

        Label footerLeft = new Label("LEDGER STATUS: IN-MEMORY DEMO");
        footerLeft.getStyleClass().add("footer-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label footerRight = new Label("PERSISTENCE: SQLITE (ACTIVE)");
        footerRight.getStyleClass().add("footer-label-secondary");

        footer.getChildren().addAll(footerLeft, spacer, footerRight);

        card.getChildren().addAll(header, table, footer);
        return card;
    }

    private TableView<ActivityItem> buildActivityTable() {
        TableView<ActivityItem> table = new TableView<>();
        table.getStyleClass().add("activity-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No recent inventory activity recorded."));
        table.setFixedCellSize(40.0);

        TableColumn<ActivityItem, String> timeCol = new TableColumn<>("TIME");
        timeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTimestamp()));
        timeCol.setMinWidth(110);
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

        TableColumn<ActivityItem, String> actionCol = new TableColumn<>("EVENT");
        actionCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getAction()));
        actionCol.setMinWidth(100);
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

        TableColumn<ActivityItem, String> itemCol = new TableColumn<>("PRODUCT / SKU");
        itemCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getItemName()));
        itemCol.setMinWidth(160);
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

        TableColumn<ActivityItem, String> changeCol = new TableColumn<>("DELTA");
        changeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getQuantityChange()));
        changeCol.setMinWidth(105);
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
        statusCol.setMinWidth(95);
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

        // Bind height to items + header to eliminate blank rows
        table.prefHeightProperty().bind(
                table.fixedCellSizeProperty()
                        .multiply(Bindings.size(table.getItems()))
                        .add(36)
        );
        table.setMinHeight(Region.USE_PREF_SIZE);
        table.setMaxHeight(Region.USE_PREF_SIZE);

        return table;
    }

    private HBox buildCardHeader(String titleText, String subtitleText, String tagText) {
        HBox header = new HBox(10);
        header.getStyleClass().add("section-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox textGroup = new VBox(2);
        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");

        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("section-subtitle");
        textGroup.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label tag = new Label(tagText);
        tag.getStyleClass().add("meta-tag");

        header.getChildren().addAll(textGroup, spacer, tag);
        return header;
    }
}
