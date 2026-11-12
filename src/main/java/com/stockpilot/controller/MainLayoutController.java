package com.stockpilot.controller;

import com.stockpilot.util.ThemeManager;
import com.stockpilot.util.ViewHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Coordinates the application shell: Industrial Minimalism sidebar,
 * top navigation bar with dual theme toggle, and dynamic view routing.
 */
public class MainLayoutController {

    private final BorderPane rootLayout;
    private final DashboardController dashboardController;
    private final ProductController productController;
    private final TransactionController transactionController;
    private final Map<String, Button> navigationButtons = new LinkedHashMap<>();
    private final Map<String, Supplier<Node>> viewFactories = new LinkedHashMap<>();

    private Label topBarTitle;
    private Label topBarSubtitle;
    private Button lightThemeBtn;
    private Button darkThemeBtn;

    public MainLayoutController() {
        this.rootLayout = new BorderPane();
        this.rootLayout.getStyleClass().add("app-root");
        this.dashboardController = new DashboardController();
        this.productController = new ProductController();
        this.transactionController = new TransactionController();

        registerViews();
        rootLayout.setLeft(buildSidebar());
        rootLayout.setTop(buildTopBar());

        // Default landing view
        navigateTo("Dashboard", "Operational metrics, inventory velocity, and category distribution");

        // Sync theme toggle buttons on theme change
        ThemeManager.addThemeListener(this::updateThemeToggleVisuals);
    }

    public BorderPane getRootLayout() {
        return rootLayout;
    }

    private void registerViews() {
        viewFactories.put("Dashboard", dashboardController::createView);
        viewFactories.put("Products", productController::createView);
        viewFactories.put("Transactions", transactionController::createView);
        viewFactories.put("Suppliers", () -> ViewHelper.createPlaceholderView(
                "Suppliers Directory",
                "PLANNED MODULE",
                "Vendor accounts, purchasing contacts, lead-time indices, and reliability scorecards will be integrated with SQLite storage."
        ));
        viewFactories.put("Ripple Intelligence", () -> ViewHelper.createPlaceholderView(
                "Ripple Intelligence",
                "COMING SOON",
                "Automated demand forecasting, smart safety stock calculation, and anomalous stock movement detection will arrive in a future update."
        ));
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(6);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(248);
        sidebar.setMinWidth(232);
        sidebar.setPadding(new Insets(20, 14, 16, 14));

        // Brand Identity Box
        VBox brandBox = new VBox(3);
        brandBox.getStyleClass().add("brand-box");
        brandBox.setPadding(new Insets(0, 8, 18, 8));

        HBox brandHeader = new HBox(8);
        brandHeader.setAlignment(Pos.CENTER_LEFT);

        Node brandIcon = ViewHelper.createIcon(ViewHelper.ICON_PRODUCTS, "brand-icon", 0.75);
        Label brandName = new Label("STOCKPILOT");
        brandName.getStyleClass().add("brand-title");

        brandHeader.getChildren().addAll(brandIcon, brandName);

        Label brandSubtitle = new Label("INVENTORY CONTROL // OS");
        brandSubtitle.getStyleClass().add("brand-subtitle");

        brandBox.getChildren().addAll(brandHeader, brandSubtitle);

        Label navHeader = new Label("CONSOLE NAVIGATION");
        navHeader.getStyleClass().add("nav-section-label");
        navHeader.setPadding(new Insets(6, 10, 6, 10));

        sidebar.getChildren().addAll(brandBox, navHeader);

        // Navigation Items with Icons
        addNavButton(sidebar, "Dashboard", "Dashboard", ViewHelper.ICON_DASHBOARD,
                "Operational metrics, inventory velocity, and category distribution", null);

        addNavButton(sidebar, "Products", "Products", ViewHelper.ICON_PRODUCTS,
                "Manage product records, categories, and stock thresholds", null);

        addNavButton(sidebar, "Transactions", "Transactions", ViewHelper.ICON_TRANSACTIONS,
                "Track stock-in receipts, dispatches, and adjustment logs", null);

        addNavButton(sidebar, "Suppliers", "Suppliers", ViewHelper.ICON_SUPPLIERS,
                "Vendor directory, purchase contacts, and lead times", null);

        addNavButton(sidebar, "Ripple Intelligence", "Ripple Intelligence", ViewHelper.ICON_RIPPLE,
                "Predictive inventory insights and risk alerts", "SOON");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Sidebar Footer
        VBox footerBox = new VBox(4);
        footerBox.getStyleClass().add("sidebar-footer");

        HBox footerTop = new HBox(6);
        footerTop.setAlignment(Pos.CENTER_LEFT);
        Label dot = new Label("●");
        dot.getStyleClass().add("system-live-dot");
        Label versionLabel = new Label("STOCKPILOT v1.0.0");
        versionLabel.getStyleClass().add("sidebar-footer-title");
        footerTop.getChildren().addAll(dot, versionLabel);

        Label runtimeLabel = new Label("RUNTIME: JAVA 25 LTS (FX25)");
        runtimeLabel.getStyleClass().add("sidebar-footer-subtitle");

        Label storageLabel = new Label("STORAGE: SQLITE (ACTIVE)");
        storageLabel.getStyleClass().add("sidebar-footer-subtitle");

        footerBox.getChildren().addAll(footerTop, runtimeLabel, storageLabel);

        sidebar.getChildren().addAll(spacer, footerBox);
        return sidebar;
    }

    private void addNavButton(VBox sidebar, String viewKey, String labelText, String iconPath,
                              String subtitle, String badgeText) {
        Button button = new Button();
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);

        HBox content = new HBox(10);
        content.setAlignment(Pos.CENTER_LEFT);

        Node icon = ViewHelper.createIcon(iconPath, "nav-icon", 0.72);
        Label label = new Label(labelText);
        label.getStyleClass().add("nav-label");

        content.getChildren().addAll(icon, label);

        if (badgeText != null) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label badge = new Label(badgeText);
            badge.getStyleClass().add("nav-badge");
            content.getChildren().addAll(spacer, badge);
            button.getStyleClass().add("nav-button-muted");
        }

        button.setGraphic(content);
        button.setOnAction(event -> navigateTo(viewKey, subtitle));

        navigationButtons.put(viewKey, button);
        sidebar.getChildren().add(button);
    }

    private HBox buildTopBar() {
        HBox topBar = new HBox(16);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(14, 28, 14, 28));

        // Header Title Hierarchy
        VBox titleContainer = new VBox(1);
        Label breadcrumb = new Label("OPERATIONS // STATION 01");
        breadcrumb.getStyleClass().add("top-bar-breadcrumb");

        topBarTitle = new Label("Dashboard");
        topBarTitle.getStyleClass().add("top-bar-title");

        topBarSubtitle = new Label("Operational metrics, inventory velocity, and category distribution");
        topBarSubtitle.getStyleClass().add("top-bar-subtitle");

        titleContainer.getChildren().addAll(breadcrumb, topBarTitle, topBarSubtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Search Field with Vector Icon
        HBox searchContainer = new HBox(8);
        searchContainer.getStyleClass().add("search-box-container");
        searchContainer.setAlignment(Pos.CENTER_LEFT);

        Node searchIcon = ViewHelper.createIcon(ViewHelper.ICON_SEARCH, "search-icon", 0.7);
        TextField searchField = new TextField();
        searchField.setPromptText("Filter SKU, item, or batch...");
        searchField.getStyleClass().add("top-search-field");
        searchField.setPrefWidth(210);

        searchContainer.getChildren().addAll(searchIcon, searchField);

        // Station / Mode Indicator Chip
        HBox stationChip = new HBox(6);
        stationChip.getStyleClass().add("station-chip");
        stationChip.setAlignment(Pos.CENTER_LEFT);

        Label stationDot = new Label("●");
        stationDot.getStyleClass().add("station-dot");

        Label stationText = new Label("WARC-01 // DEMO DATA");
        stationText.getStyleClass().add("station-text");

        stationChip.getChildren().addAll(stationDot, stationText);

        // Elegant Dual Theme Toggle Switch
        HBox themeToggleGroup = buildThemeToggle();

        topBar.getChildren().addAll(titleContainer, spacer, searchContainer, stationChip, themeToggleGroup);
        return topBar;
    }

    private HBox buildThemeToggle() {
        HBox toggleContainer = new HBox(2);
        toggleContainer.getStyleClass().add("theme-toggle-container");
        toggleContainer.setAlignment(Pos.CENTER);

        lightThemeBtn = new Button();
        lightThemeBtn.getStyleClass().add("theme-toggle-btn");
        lightThemeBtn.setGraphic(ViewHelper.createIcon(ViewHelper.ICON_SUN, "theme-toggle-icon", 0.7));
        lightThemeBtn.setTooltip(new Tooltip("Switch to Light Industrial theme"));
        lightThemeBtn.setOnAction(e -> ThemeManager.setTheme(ThemeManager.Theme.LIGHT));

        darkThemeBtn = new Button();
        darkThemeBtn.getStyleClass().add("theme-toggle-btn");
        darkThemeBtn.setGraphic(ViewHelper.createIcon(ViewHelper.ICON_MOON, "theme-toggle-icon", 0.7));
        darkThemeBtn.setTooltip(new Tooltip("Switch to Dark Industrial theme"));
        darkThemeBtn.setOnAction(e -> ThemeManager.setTheme(ThemeManager.Theme.DARK));

        toggleContainer.getChildren().addAll(lightThemeBtn, darkThemeBtn);
        updateThemeToggleVisuals(ThemeManager.getCurrentTheme());
        return toggleContainer;
    }

    private void updateThemeToggleVisuals(ThemeManager.Theme theme) {
        if (lightThemeBtn == null || darkThemeBtn == null) {
            return;
        }

        if (theme == ThemeManager.Theme.LIGHT) {
            if (!lightThemeBtn.getStyleClass().contains("theme-toggle-active")) {
                lightThemeBtn.getStyleClass().add("theme-toggle-active");
            }
            darkThemeBtn.getStyleClass().remove("theme-toggle-active");
        } else {
            if (!darkThemeBtn.getStyleClass().contains("theme-toggle-active")) {
                darkThemeBtn.getStyleClass().add("theme-toggle-active");
            }
            lightThemeBtn.getStyleClass().remove("theme-toggle-active");
        }
    }

    private void navigateTo(String viewKey, String subtitle) {
        Supplier<Node> factory = viewFactories.get(viewKey);
        if (factory == null) {
            return;
        }

        rootLayout.setCenter(factory.get());
        if (topBarTitle != null) {
            topBarTitle.setText(viewKey);
            topBarSubtitle.setText(subtitle);
        }

        for (Map.Entry<String, Button> entry : navigationButtons.entrySet()) {
            entry.getValue().getStyleClass().remove("nav-button-active");
            if (entry.getKey().equals(viewKey)) {
                entry.getValue().getStyleClass().add("nav-button-active");
            }
        }
    }
}
