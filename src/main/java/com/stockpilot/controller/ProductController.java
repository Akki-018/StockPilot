package com.stockpilot.controller;

import com.stockpilot.model.Product;
import com.stockpilot.service.ProductService;
import com.stockpilot.util.ThemeManager;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Controller for the Products page.
 * Provides complete inventory product management: viewing, searching, filtering,
 * adding, editing, and deleting products with full SQLite persistence.
 */
public class ProductController {

    private final ProductService productService;
    private final ObservableList<Product> productList = FXCollections.observableArrayList();
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);

    private TableView<Product> tableView;
    private TextField searchField;
    private ComboBox<String> categoryFilter;
    private ComboBox<String> statusFilter;
    private Label recordCountLabel;
    private HBox notificationBanner;
    private Label notificationMessage;

    public ProductController() {
        this.productService = new ProductService();
    }

    public Node createView() {
        VBox root = new VBox(16);
        root.getStyleClass().add("content-container");
        root.setPadding(new Insets(22, 26, 26, 26));

        // 1. Header with Title and Primary Actions
        HBox header = buildHeader();

        // 2. Notification / Feedback Banner (Hidden by default)
        notificationBanner = buildNotificationBanner();

        // 3. Filter and Search Toolbar
        HBox filterToolbar = buildFilterToolbar();

        // 4. Products TableView
        tableView = buildTableView();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        root.getChildren().addAll(header, notificationBanner, filterToolbar, tableView);

        // Initial Data Load
        refreshData();

        return root;
    }

    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox textGroup = new VBox(2);
        Label title = new Label("PRODUCTS CATALOG");
        title.getStyleClass().add("section-title");

        Label subtitle = new Label("Manage SKU records, category classifications, unit volumes, and reorder points");
        subtitle.getStyleClass().add("section-subtitle");
        textGroup.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> refreshData());

        Button addProductBtn = new Button("+ Add Product");
        addProductBtn.getStyleClass().add("btn-primary");
        addProductBtn.setOnAction(e -> openAddProductDialog());

        header.getChildren().addAll(textGroup, spacer, refreshBtn, addProductBtn);
        return header;
    }

    private HBox buildNotificationBanner() {
        HBox banner = new HBox(10);
        banner.getStyleClass().add("notification-banner");
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(9, 14, 9, 14));
        banner.setVisible(false);
        banner.setManaged(false);

        notificationMessage = new Label();
        notificationMessage.getStyleClass().add("notification-message");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button dismissBtn = new Button("×");
        dismissBtn.getStyleClass().add("notification-dismiss-btn");
        dismissBtn.setOnAction(e -> hideNotification());

        banner.getChildren().addAll(notificationMessage, spacer, dismissBtn);
        return banner;
    }

    private void showNotification(String message, boolean isSuccess) {
        notificationMessage.setText(message);
        notificationBanner.getStyleClass().removeAll("banner-success", "banner-danger");
        notificationBanner.getStyleClass().add(isSuccess ? "banner-success" : "banner-danger");
        notificationBanner.setVisible(true);
        notificationBanner.setManaged(true);
    }

    private void hideNotification() {
        notificationBanner.setVisible(false);
        notificationBanner.setManaged(false);
    }

    private HBox buildFilterToolbar() {
        HBox toolbar = new HBox(12);
        toolbar.getStyleClass().add("filter-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(10, 14, 10, 14));

        searchField = new TextField();
        searchField.setPromptText("Filter by Name or SKU...");
        searchField.getStyleClass().add("top-search-field");
        searchField.setPrefWidth(240);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        categoryFilter = new ComboBox<>();
        categoryFilter.getStyleClass().add("filter-combo");
        categoryFilter.setPromptText("Category");
        categoryFilter.setPrefWidth(160);
        categoryFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        statusFilter = new ComboBox<>();
        statusFilter.getStyleClass().add("filter-combo");
        statusFilter.getItems().addAll("All Statuses", "Healthy", "Low Stock", "Out of Stock");
        statusFilter.setValue("All Statuses");
        statusFilter.setPrefWidth(140);
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        Button resetBtn = new Button("Reset");
        resetBtn.getStyleClass().add("btn-secondary");
        resetBtn.setOnAction(e -> resetFilters());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        recordCountLabel = new Label("0 PRODUCTS");
        recordCountLabel.getStyleClass().add("meta-tag");

        toolbar.getChildren().addAll(searchField, categoryFilter, statusFilter, resetBtn, spacer, recordCountLabel);
        return toolbar;
    }

    private TableView<Product> buildTableView() {
        TableView<Product> table = new TableView<>(productList);
        table.getStyleClass().add("activity-table");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No products match the selected search or filter criteria."));

        // Column 1: SKU
        TableColumn<Product, String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSku()));
        skuCol.setMinWidth(110);
        skuCol.setPrefWidth(130);
        skuCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String sku, boolean empty) {
                super.updateItem(sku, empty);
                if (empty || sku == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(sku);
                    getStyleClass().add("cell-sku");
                    setTooltip(new Tooltip("SKU: " + sku));
                }
            }
        });

        // Column 2: Product Name
        TableColumn<Product, String> nameCol = new TableColumn<>("PRODUCT NAME");
        nameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        nameCol.setMinWidth(170);
        nameCol.setPrefWidth(220);
        nameCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(name);
                    getStyleClass().add("cell-item-name");
                    setTooltip(new Tooltip(name));
                }
            }
        });

        // Column 3: Category
        TableColumn<Product, String> catCol = new TableColumn<>("CATEGORY");
        catCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCategory()));
        catCol.setMinWidth(100);
        catCol.setPrefWidth(125);
        catCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String cat, boolean empty) {
                super.updateItem(cat, empty);
                if (empty || cat == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label pill = new Label(cat);
                    pill.getStyleClass().add("action-tag");
                    setGraphic(pill);
                    setText(null);
                }
            }
        });

        // Column 4: Quantity
        TableColumn<Product, Integer> qtyCol = new TableColumn<>("QTY");
        qtyCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getQuantity()));
        qtyCol.setMinWidth(65);
        qtyCol.setPrefWidth(75);
        qtyCol.setStyle("-fx-alignment: CENTER_RIGHT;");
        qtyCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer qty, boolean empty) {
                super.updateItem(qty, empty);
                if (empty || qty == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(qty));
                    getStyleClass().add("cell-numeric");
                }
            }
        });

        // Column 5: Min Stock Threshold
        TableColumn<Product, Integer> minStockCol = new TableColumn<>("MIN");
        minStockCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getMinimumStock()));
        minStockCol.setMinWidth(65);
        minStockCol.setPrefWidth(75);
        minStockCol.setStyle("-fx-alignment: CENTER_RIGHT;");
        minStockCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer min, boolean empty) {
                super.updateItem(min, empty);
                if (empty || min == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(min));
                    getStyleClass().add("cell-numeric-muted");
                }
            }
        });

        // Column 6: Unit Price
        TableColumn<Product, BigDecimal> priceCol = new TableColumn<>("PRICE");
        priceCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getUnitPrice()));
        priceCol.setMinWidth(80);
        priceCol.setPrefWidth(95);
        priceCol.setStyle("-fx-alignment: CENTER_RIGHT;");
        priceCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(currencyFormatter.format(price));
                    getStyleClass().add("cell-currency");
                }
            }
        });

        // Column 7: Total Value
        TableColumn<Product, BigDecimal> valCol = new TableColumn<>("TOTAL VAL");
        valCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getTotalValuation()));
        valCol.setMinWidth(90);
        valCol.setPrefWidth(110);
        valCol.setStyle("-fx-alignment: CENTER_RIGHT;");
        valCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setText(null);
                } else {
                    setText(currencyFormatter.format(val));
                    getStyleClass().add("cell-currency");
                }
            }
        });

        // Column 8: Status
        TableColumn<Product, String> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStockStatus()));
        statusCol.setMinWidth(100);
        statusCol.setPrefWidth(115);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(status.toUpperCase());
                badge.getStyleClass().add("status-pill");
                switch (status) {
                    case "Healthy" -> badge.getStyleClass().add("status-completed");
                    case "Low Stock" -> badge.getStyleClass().add("status-attention");
                    case "Out of Stock" -> badge.getStyleClass().add("status-critical");
                    default -> badge.getStyleClass().add("status-neutral");
                }
                setGraphic(badge);
                setText(null);
            }
        });

        // Column 9: Actions (Edit / Delete)
        TableColumn<Product, Void> actionCol = new TableColumn<>("ACTIONS");
        actionCol.setMinWidth(155);
        actionCol.setPrefWidth(165);
        actionCol.setResizable(false);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox btnGroup = new HBox(6, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("btn-table-edit");
                deleteBtn.getStyleClass().add("btn-table-delete");
                editBtn.setMinWidth(Region.USE_PREF_SIZE);
                deleteBtn.setMinWidth(Region.USE_PREF_SIZE);
                btnGroup.setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setAlignment(Pos.CENTER);

                editBtn.setOnAction(e -> {
                    Product prod = getTableView().getItems().get(getIndex());
                    openEditProductDialog(prod);
                });

                deleteBtn.setOnAction(e -> {
                    Product prod = getTableView().getItems().get(getIndex());
                    confirmAndDeleteProduct(prod);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnGroup);
            }
        });

        table.getColumns().add(skuCol);
        table.getColumns().add(nameCol);
        table.getColumns().add(catCol);
        table.getColumns().add(qtyCol);
        table.getColumns().add(minStockCol);
        table.getColumns().add(priceCol);
        table.getColumns().add(valCol);
        table.getColumns().add(statusCol);
        table.getColumns().add(actionCol);

        return table;
    }

    public void refreshData() {
        // Refresh Category dropdown choices
        List<String> categories = productService.getAllCategories();
        String currentCat = categoryFilter.getValue();
        categoryFilter.getItems().clear();
        categoryFilter.getItems().add("All Categories");
        categoryFilter.getItems().addAll(categories);
        if (currentCat != null && categoryFilter.getItems().contains(currentCat)) {
            categoryFilter.setValue(currentCat);
        } else {
            categoryFilter.setValue("All Categories");
        }

        applyFilters();
    }

    private void applyFilters() {
        String query = searchField != null ? searchField.getText() : null;
        String cat = categoryFilter != null ? categoryFilter.getValue() : null;
        String status = statusFilter != null ? statusFilter.getValue() : null;

        List<Product> results = productService.searchAndFilter(query, cat, status);
        productList.setAll(results);

        if (recordCountLabel != null) {
            recordCountLabel.setText(results.size() + " PRODUCTS");
        }
    }

    private void resetFilters() {
        if (searchField != null) searchField.clear();
        if (categoryFilter != null) categoryFilter.setValue("All Categories");
        if (statusFilter != null) statusFilter.setValue("All Statuses");
        applyFilters();
    }

    private void openAddProductDialog() {
        ProductDialog dialog = new ProductDialog(null, productService.getAllCategories());
        Optional<Product> result = dialog.showAndWait();
        result.ifPresent(product -> {
            try {
                productService.saveProduct(product);
                showNotification("Product '" + product.getSku() + "' added successfully.", true);
                refreshData();
            } catch (IllegalArgumentException e) {
                showNotification("Validation Error: " + e.getMessage(), false);
            } catch (SQLException e) {
                showNotification("Database Error: " + e.getMessage(), false);
            }
        });
    }

    private void openEditProductDialog(Product product) {
        ProductDialog dialog = new ProductDialog(product, productService.getAllCategories());
        Optional<Product> result = dialog.showAndWait();
        result.ifPresent(updatedProduct -> {
            try {
                productService.saveProduct(updatedProduct);
                showNotification("Product '" + updatedProduct.getSku() + "' updated successfully.", true);
                refreshData();
            } catch (IllegalArgumentException e) {
                showNotification("Validation Error: " + e.getMessage(), false);
            } catch (SQLException e) {
                showNotification("Database Error: " + e.getMessage(), false);
            }
        });
    }

    private void confirmAndDeleteProduct(Product product) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Product: " + product.getSku());
        alert.setContentText("Are you sure you want to permanently delete '" + product.getName() + "'? This operation cannot be reversed.");

        // Apply active theme to confirmation dialog
        applyDialogTheme(alert.getDialogPane());
        Button okBtn = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        if (okBtn != null) {
            okBtn.setText("Delete Product");
            okBtn.getStyleClass().add("btn-table-delete");
        }
        Button cancelBtn = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null) {
            cancelBtn.getStyleClass().add("btn-secondary");
        }

        Optional<ButtonType> response = alert.showAndWait();
        if (response.isPresent() && response.get() == ButtonType.OK) {
            try {
                boolean deleted = productService.deleteProduct(product.getId());
                if (deleted) {
                    showNotification("Product '" + product.getSku() + "' deleted successfully.", true);
                    refreshData();
                } else {
                    showNotification("Could not delete product. Record may not exist.", false);
                }
            } catch (IllegalStateException e) {
                showNotification("Integrity Protection: " + e.getMessage(), false);
            } catch (SQLException e) {
                showNotification("Database Error: Unable to delete product: " + e.getMessage(), false);
            }
        }
    }

    private static void applyDialogTheme(javafx.scene.control.DialogPane dialogPane) {
        dialogPane.getStylesheets().clear();
        String baseCss = ProductController.class.getResource("/styles/base.css").toExternalForm();
        String themeCss = ProductController.class.getResource(ThemeManager.getCurrentTheme().getStylesheetPath()).toExternalForm();
        dialogPane.getStylesheets().addAll(baseCss, themeCss);
        dialogPane.getStyleClass().add("stockpilot-dialog");
    }

    /**
     * Inner dialog component for Add / Edit Product modal forms.
     */
    private static class ProductDialog extends Dialog<Product> {

        private final Product existingProduct;
        private final TextField skuField = new TextField();
        private final TextField nameField = new TextField();
        private final ComboBox<String> categoryCombo = new ComboBox<>();
        private final TextField quantityField = new TextField();
        private final TextField minStockField = new TextField();
        private final TextField unitPriceField = new TextField();
        private final TextArea descriptionArea = new TextArea();
        private final Label validationErrorLabel = new Label();

        public ProductDialog(Product product, List<String> availableCategories) {
            this.existingProduct = product;
            boolean isEdit = (product != null);

            setTitle(isEdit ? "Edit Product // " + product.getSku() : "Add New Inventory Product");
            setHeaderText(isEdit ? "Update SKU record and stock thresholds" : "Create a new catalog item in SQLite storage");

            // Setup buttons
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
            okButton.setText(isEdit ? "Save Changes" : "Create Product");
            okButton.getStyleClass().add("btn-primary");

            Button cancelButton = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
            cancelButton.getStyleClass().add("btn-secondary");

            // Category options
            categoryCombo.setEditable(true);
            categoryCombo.getItems().addAll(availableCategories);
            categoryCombo.setMaxWidth(Double.MAX_VALUE);

            // Populate existing fields if edit
            if (isEdit) {
                skuField.setText(product.getSku());
                nameField.setText(product.getName());
                categoryCombo.setValue(product.getCategory());
                quantityField.setText(String.valueOf(product.getQuantity()));
                minStockField.setText(String.valueOf(product.getMinimumStock()));
                unitPriceField.setText(product.getUnitPrice().toString());
                descriptionArea.setText(product.getDescription());
            } else {
                quantityField.setText("0");
                minStockField.setText("10");
                unitPriceField.setText("0.00");
                if (!availableCategories.isEmpty()) {
                    categoryCombo.setValue(availableCategories.get(0));
                }
            }

            descriptionArea.setPrefRowCount(3);
            descriptionArea.setWrapText(true);

            // Prompt texts for clean placeholder contrast
            skuField.setPromptText("e.g. PRD-1001");
            nameField.setPromptText("e.g. High-Torque Stepper Motor");
            categoryCombo.setPromptText("Select or enter category...");
            quantityField.setPromptText("e.g. 50");
            minStockField.setPromptText("e.g. 10");
            unitPriceField.setPromptText("e.g. 49.99");
            descriptionArea.setPromptText("Optional specifications, supplier reference, or notes...");

            validationErrorLabel.getStyleClass().add("dialog-error-label");
            validationErrorLabel.setVisible(false);
            validationErrorLabel.setManaged(false);

            GridPane grid = new GridPane();
            grid.setHgap(12);
            grid.setVgap(10);
            grid.setPadding(new Insets(16, 20, 16, 20));

            int row = 0;
            grid.add(createFormLabel("SKU Code:*"), 0, row);
            grid.add(skuField, 1, row++);

            grid.add(createFormLabel("Product Name:*"), 0, row);
            grid.add(nameField, 1, row++);

            grid.add(createFormLabel("Category:*"), 0, row);
            grid.add(categoryCombo, 1, row++);

            grid.add(createFormLabel("Quantity on Hand:*"), 0, row);
            grid.add(quantityField, 1, row++);

            grid.add(createFormLabel("Min. Stock Buffer:*"), 0, row);
            grid.add(minStockField, 1, row++);

            grid.add(createFormLabel("Unit Price ($):*"), 0, row);
            grid.add(unitPriceField, 1, row++);

            grid.add(createFormLabel("Description:"), 0, row);
            grid.add(descriptionArea, 1, row++);

            grid.add(validationErrorLabel, 0, row, 2, 1);

            getDialogPane().setContent(grid);
            applyDialogTheme(getDialogPane());

            // Custom validation interception on OK button
            okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                String error = validateForm();
                if (error != null) {
                    event.consume(); // keep dialog open
                    validationErrorLabel.setText("⚠ " + error);
                    validationErrorLabel.setVisible(true);
                    validationErrorLabel.setManaged(true);
                }
            });

            // Convert result
            setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    Product result = isEdit ? existingProduct : new Product();
                    result.setSku(skuField.getText().trim().toUpperCase());
                    result.setName(nameField.getText().trim());
                    result.setCategory(categoryCombo.getValue() != null ? categoryCombo.getValue().trim() : "");
                    result.setQuantity(Integer.parseInt(quantityField.getText().trim()));
                    result.setMinimumStock(Integer.parseInt(minStockField.getText().trim()));
                    result.setUnitPrice(new BigDecimal(unitPriceField.getText().trim()).setScale(2, RoundingMode.HALF_UP));
                    result.setDescription(descriptionArea.getText() != null ? descriptionArea.getText().trim() : "");
                    return result;
                }
                return null;
            });
        }

        private String validateForm() {
            String sku = skuField.getText();
            if (sku == null || sku.trim().isEmpty()) {
                return "SKU is required.";
            }
            if (sku.trim().length() < 3 || sku.trim().length() > 30) {
                return "SKU must be between 3 and 30 characters.";
            }

            String name = nameField.getText();
            if (name == null || name.trim().isEmpty()) {
                return "Product name is required.";
            }

            String cat = categoryCombo.getValue();
            if (cat == null || cat.trim().isEmpty()) {
                return "Please select or enter a category.";
            }

            try {
                int qty = Integer.parseInt(quantityField.getText().trim());
                if (qty < 0) return "Quantity cannot be negative.";
            } catch (NumberFormatException e) {
                return "Quantity must be a valid integer.";
            }

            try {
                int min = Integer.parseInt(minStockField.getText().trim());
                if (min < 0) return "Minimum stock threshold cannot be negative.";
            } catch (NumberFormatException e) {
                return "Minimum stock threshold must be a valid integer.";
            }

            try {
                BigDecimal price = new BigDecimal(unitPriceField.getText().trim());
                if (price.compareTo(BigDecimal.ZERO) < 0) return "Unit price cannot be negative.";
            } catch (Exception e) {
                return "Unit price must be a valid monetary value (e.g. 19.99).";
            }

            return null; // Valid
        }

        private static Label createFormLabel(String text) {
            Label label = new Label(text);
            label.getStyleClass().add("form-label");
            return label;
        }
    }
}
