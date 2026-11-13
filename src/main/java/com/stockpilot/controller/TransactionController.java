package com.stockpilot.controller;

import com.stockpilot.model.Product;
import com.stockpilot.model.Transaction;
import com.stockpilot.service.InventoryService;
import com.stockpilot.service.ProductService;
import com.stockpilot.util.ThemeManager;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the Transactions module.
 * Provides atomic Stock In and Stock Out operations and a filtered audit ledger TableView.
 */
public class TransactionController {

    private final InventoryService inventoryService;
    private final ProductService productService;
    private final ObservableList<Transaction> transactionList = FXCollections.observableArrayList();

    private TableView<Transaction> tableView;
    private TextField searchField;
    private ComboBox<String> typeFilter;
    private ComboBox<String> dateFilter;
    private Label recordCountLabel;
    private HBox notificationBanner;
    private Label notificationMessage;

    public TransactionController() {
        this.inventoryService = new InventoryService();
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

        // 4. Transactions TableView
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
        Label title = new Label("TRANSACTIONS LEDGER");
        title.getStyleClass().add("section-title");

        Label subtitle = new Label("Audit trail of inbound stock receipts, outbound dispatches, and inventory adjustments");
        subtitle.getStyleClass().add("section-subtitle");
        textGroup.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> refreshData());

        Button stockOutBtn = new Button("- Stock Out");
        stockOutBtn.getStyleClass().add("btn-stock-out");
        stockOutBtn.setOnAction(e -> openStockOperationDialog(false));

        Button stockInBtn = new Button("+ Stock In");
        stockInBtn.getStyleClass().add("btn-stock-in");
        stockInBtn.setOnAction(e -> openStockOperationDialog(true));

        header.getChildren().addAll(textGroup, spacer, refreshBtn, stockOutBtn, stockInBtn);
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
        searchField.setPromptText("Filter by SKU, Name, or Notes...");
        searchField.getStyleClass().add("top-search-field");
        searchField.setPrefWidth(240);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        typeFilter = new ComboBox<>();
        typeFilter.getStyleClass().add("filter-combo");
        typeFilter.getItems().addAll("All Types", "Stock In", "Stock Out");
        typeFilter.setValue("All Types");
        typeFilter.setPrefWidth(130);
        typeFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        dateFilter = new ComboBox<>();
        dateFilter.getStyleClass().add("filter-combo");
        dateFilter.getItems().addAll("All Time", "Today", "Last 7 Days", "Last 30 Days");
        dateFilter.setValue("All Time");
        dateFilter.setPrefWidth(140);
        dateFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        Button resetBtn = new Button("Reset");
        resetBtn.getStyleClass().add("btn-secondary");
        resetBtn.setOnAction(e -> resetFilters());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        recordCountLabel = new Label("0 TRANSACTIONS");
        recordCountLabel.getStyleClass().add("meta-tag");

        toolbar.getChildren().addAll(searchField, typeFilter, dateFilter, resetBtn, spacer, recordCountLabel);
        return toolbar;
    }

    private TableView<Transaction> buildTableView() {
        TableView<Transaction> table = new TableView<>(transactionList);
        table.getStyleClass().add("activity-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No inventory transactions match the selected criteria."));

        // Column 1: ID
        TableColumn<Transaction, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cell -> new SimpleStringProperty(String.format("#%03d", cell.getValue().getId())));
        idCol.setMinWidth(65);
        idCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    getStyleClass().add("cell-sku");
                }
            }
        });

        // Column 2: Date & Time
        TableColumn<Transaction, String> dateCol = new TableColumn<>("DATE & TIME");
        dateCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTransactionDate()));
        dateCol.setMinWidth(140);
        dateCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    getStyleClass().add("cell-timestamp");
                }
            }
        });

        // Column 3: SKU
        TableColumn<Transaction, String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProductSku()));
        skuCol.setMinWidth(120);
        skuCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String sku, boolean empty) {
                super.updateItem(sku, empty);
                if (empty || sku == null) {
                    setText(null);
                } else {
                    setText(sku);
                    getStyleClass().add("cell-sku");
                }
            }
        });

        // Column 4: Product Name
        TableColumn<Transaction, String> nameCol = new TableColumn<>("PRODUCT NAME");
        nameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProductName()));
        nameCol.setMinWidth(170);
        nameCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) {
                    setText(null);
                } else {
                    setText(name);
                    getStyleClass().add("cell-item-name");
                }
            }
        });

        // Column 5: Transaction Type
        TableColumn<Transaction, String> typeCol = new TableColumn<>("TYPE");
        typeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFormattedType()));
        typeCol.setMinWidth(115);
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label tag = new Label(type);
                tag.getStyleClass().add("action-tag");
                if (type.contains("IN")) {
                    tag.setText("▲ " + type);
                    tag.getStyleClass().add("action-tag-in");
                } else {
                    tag.setText("▼ " + type);
                    tag.getStyleClass().add("action-tag-out");
                }
                setGraphic(tag);
                setText(null);
            }
        });

        // Column 6: Quantity
        TableColumn<Transaction, Integer> qtyCol = new TableColumn<>("QTY");
        qtyCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getQuantity()));
        qtyCol.setMinWidth(75);
        qtyCol.setStyle("-fx-alignment: CENTER_RIGHT;");
        qtyCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer qty, boolean empty) {
                super.updateItem(qty, empty);
                if (empty || qty == null) {
                    setText(null);
                } else {
                    Transaction tx = getTableRow() != null ? getTableRow().getItem() : null;
                    if (tx != null && tx.isStockIn()) {
                        setText("+" + qty);
                        setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold; -fx-text-fill: #27683C;");
                    } else {
                        setText("-" + qty);
                        setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold; -fx-text-fill: #C8784B;");
                    }
                }
            }
        });

        // Column 7: Previous Stock
        TableColumn<Transaction, Integer> prevCol = new TableColumn<>("PREV");
        prevCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getPreviousStock()));
        prevCol.setMinWidth(75);
        prevCol.setStyle("-fx-alignment: CENTER_RIGHT;");

        // Column 8: Resulting Stock
        TableColumn<Transaction, Integer> newCol = new TableColumn<>("BALANCE");
        newCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getResultingStock()));
        newCol.setMinWidth(85);
        newCol.setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold;");

        // Column 9: Notes
        TableColumn<Transaction, String> notesCol = new TableColumn<>("AUDIT NOTES");
        notesCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNotes()));
        notesCol.setMinWidth(180);

        table.getColumns().add(idCol);
        table.getColumns().add(dateCol);
        table.getColumns().add(skuCol);
        table.getColumns().add(nameCol);
        table.getColumns().add(typeCol);
        table.getColumns().add(qtyCol);
        table.getColumns().add(prevCol);
        table.getColumns().add(newCol);
        table.getColumns().add(notesCol);

        return table;
    }

    public void refreshData() {
        applyFilters();
    }

    private void applyFilters() {
        String query = searchField != null ? searchField.getText() : null;
        String type = typeFilter != null ? typeFilter.getValue() : null;
        String datePreset = dateFilter != null ? dateFilter.getValue() : null;

        List<Transaction> results = inventoryService.searchAndFilter(query, type, datePreset);
        transactionList.setAll(results);

        if (recordCountLabel != null) {
            recordCountLabel.setText(results.size() + " TRANSACTIONS");
        }
    }

    private void resetFilters() {
        if (searchField != null) searchField.clear();
        if (typeFilter != null) typeFilter.setValue("All Types");
        if (dateFilter != null) dateFilter.setValue("All Time");
        applyFilters();
    }

    /**
     * Opens the modal dialog for Stock In or Stock Out.
     */
    private void openStockOperationDialog(boolean isStockIn) {
        List<Product> products = productService.getAllProducts();
        if (products.isEmpty()) {
            showNotification("No products found in catalog. Create a product before recording transactions.", false);
            return;
        }

        StockOperationDialog dialog = new StockOperationDialog(isStockIn, products);
        Optional<StockOperationRequest> result = dialog.showAndWait();

        result.ifPresent(req -> {
            try {
                Transaction tx;
                if (isStockIn) {
                    tx = inventoryService.recordStockIn(req.productId(), req.quantity(), req.notes());
                    showNotification("Stock In recorded: +" + tx.getQuantity() + " units for [" + tx.getProductSku() + "]. New Balance: " + tx.getResultingStock(), true);
                } else {
                    tx = inventoryService.recordStockOut(req.productId(), req.quantity(), req.notes());
                    showNotification("Stock Out recorded: -" + tx.getQuantity() + " units for [" + tx.getProductSku() + "]. New Balance: " + tx.getResultingStock(), true);
                }
                refreshData();
            } catch (IllegalArgumentException e) {
                showNotification("Validation Error: " + e.getMessage(), false);
            } catch (SQLException e) {
                showNotification("Database Error: " + e.getMessage(), false);
            }
        });
    }

    private static void applyDialogTheme(DialogPane dialogPane) {
        dialogPane.getStylesheets().clear();
        String baseCss = TransactionController.class.getResource("/styles/base.css").toExternalForm();
        String themeCss = TransactionController.class.getResource(ThemeManager.getCurrentTheme().getStylesheetPath()).toExternalForm();
        dialogPane.getStylesheets().addAll(baseCss, themeCss);
        dialogPane.getStyleClass().add("stockpilot-dialog");
    }

    private record StockOperationRequest(int productId, int quantity, String notes) {
    }

    /**
     * Modal dialog for Stock In / Stock Out operations.
     */
    private static class StockOperationDialog extends Dialog<StockOperationRequest> {

        private final ComboBox<Product> productCombo = new ComboBox<>();
        private final Label currentStockLabel = new Label("Select a product to view stock balance.");
        private final TextField quantityField = new TextField();
        private final TextArea notesArea = new TextArea();
        private final Label errorLabel = new Label();

        public StockOperationDialog(boolean isStockIn, List<Product> products) {
            String opTitle = isStockIn ? "Stock In // Inbound Receiving" : "Stock Out // Order Dispatch";
            String opHeader = isStockIn
                    ? "Record incoming stock shipment into warehouse inventory"
                    : "Record outgoing stock issue from warehouse inventory";

            setTitle(opTitle);
            setHeaderText(opHeader);

            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
            okButton.setText(isStockIn ? "Confirm Stock In" : "Confirm Stock Out");
            okButton.getStyleClass().add(isStockIn ? "btn-stock-in" : "btn-stock-out");

            Button cancelButton = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
            cancelButton.getStyleClass().add("btn-secondary");

            // Format ComboBox product cell
            productCombo.getItems().addAll(products);
            productCombo.setMaxWidth(Double.MAX_VALUE);
            productCombo.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Product item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText("[" + item.getSku() + "] " + item.getName() + " (On Hand: " + item.getQuantity() + ")");
                    }
                }
            });
            productCombo.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(Product item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText("[" + item.getSku() + "] " + item.getName() + " (On Hand: " + item.getQuantity() + ")");
                    }
                }
            });

            productCombo.valueProperty().addListener((obs, oldVal, selectedProduct) -> {
                if (selectedProduct != null) {
                    currentStockLabel.setText(String.format("Available Stock: %d units | Min Buffer: %d | Status: %s",
                            selectedProduct.getQuantity(), selectedProduct.getMinimumStock(), selectedProduct.getStockStatus()));
                } else {
                    currentStockLabel.setText("Select a product to view stock balance.");
                }
            });

            if (!products.isEmpty()) {
                productCombo.setValue(products.get(0));
            }

            currentStockLabel.getStyleClass().add("footer-spec");

            quantityField.setPromptText("Units to " + (isStockIn ? "receive" : "issue") + " (e.g. 10)");

            notesArea.setPromptText("Optional notes, invoice #, receiving bay, or order reference...");
            notesArea.setPrefRowCount(3);
            notesArea.setWrapText(true);

            errorLabel.getStyleClass().add("dialog-error-label");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);

            GridPane grid = new GridPane();
            grid.setHgap(12);
            grid.setVgap(10);
            grid.setPadding(new Insets(16, 20, 16, 20));

            int row = 0;
            grid.add(new Label("Target Product:*"), 0, row);
            grid.add(productCombo, 1, row++);

            grid.add(new Label("Current Stock:"), 0, row);
            grid.add(currentStockLabel, 1, row++);

            grid.add(new Label(isStockIn ? "Quantity Received:* " : "Quantity Issued:* "), 0, row);
            grid.add(quantityField, 1, row++);

            grid.add(new Label("Audit Notes:"), 0, row);
            grid.add(notesArea, 1, row++);

            grid.add(errorLabel, 0, row, 2, 1);

            getDialogPane().setContent(grid);
            applyDialogTheme(getDialogPane());

            // Intercept OK button for client validation
            okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                Product selected = productCombo.getValue();
                if (selected == null) {
                    event.consume();
                    showError("Please select a product.");
                    return;
                }

                String qtyStr = quantityField.getText();
                if (qtyStr == null || qtyStr.trim().isEmpty()) {
                    event.consume();
                    showError("Quantity is required.");
                    return;
                }

                int qty;
                try {
                    qty = Integer.parseInt(qtyStr.trim());
                    if (qty <= 0) {
                        event.consume();
                        showError("Quantity must be greater than zero.");
                        return;
                    }
                } catch (NumberFormatException e) {
                    event.consume();
                    showError("Quantity must be a valid whole number.");
                    return;
                }

                if (!isStockIn && qty > selected.getQuantity()) {
                    event.consume();
                    showError(String.format("Insufficient stock. Cannot issue %d units when available stock is only %d.",
                            qty, selected.getQuantity()));
                }
            });

            setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK && productCombo.getValue() != null) {
                    int qty = Integer.parseInt(quantityField.getText().trim());
                    String notes = notesArea.getText() != null ? notesArea.getText().trim() : "";
                    return new StockOperationRequest(productCombo.getValue().getId(), qty, notes);
                }
                return null;
            });
        }

        private void showError(String msg) {
            errorLabel.setText("⚠ " + msg);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }
}

