package com.stockpilot.service;

import com.stockpilot.dao.TransactionDao;
import com.stockpilot.database.DatabaseManager;
import com.stockpilot.model.DailyMovement;
import com.stockpilot.model.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service managing atomic stock inventory movements (Stock In / Stock Out)
 * and transaction audit ledger querying.
 * Ensures strict atomicity using manual JDBC transaction control (commit & rollback).
 */
public class InventoryService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TransactionDao transactionDao;

    public InventoryService() {
        this.transactionDao = new TransactionDao();
    }

    public InventoryService(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    /**
     * Records a Stock In operation, increasing product quantity and writing an audit record.
     *
     * @param productId product primary key
     * @param quantity  units received (must be > 0)
     * @param notes     optional movement notes
     * @return persisted Transaction record
     */
    public Transaction recordStockIn(int productId, int quantity, String notes) throws SQLException {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Stock In quantity must be greater than zero.");
        }
        return executeAtomicTransaction(productId, "STOCK_IN", quantity, notes);
    }

    /**
     * Records a Stock Out operation, decreasing product quantity and writing an audit record.
     * Prevents negative stock balance.
     *
     * @param productId product primary key
     * @param quantity  units issued (must be > 0 and <= available stock)
     * @param notes     optional movement notes
     * @return persisted Transaction record
     */
    public Transaction recordStockOut(int productId, int quantity, String notes) throws SQLException {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Stock Out quantity must be greater than zero.");
        }
        return executeAtomicTransaction(productId, "STOCK_OUT", quantity, notes);
    }

    /**
     * Executes the atomic 7-step inventory transaction:
     * 1. Begin database transaction (setAutoCommit = false).
     * 2. Retrieve current product stock.
     * 3. Validate requested quantity.
     * 4. Calculate resulting stock.
     * 5. Update product quantity.
     * 6. Insert transaction record.
     * 7. Commit.
     * Rolls back on any failure.
     */
    private Transaction executeAtomicTransaction(int productId, String type, int quantity, String notes) throws SQLException {
        String selectProductSql = "SELECT id, sku, name, quantity FROM products WHERE id = ?";
        String updateProductSql = "UPDATE products SET quantity = ?, updated_at = ? WHERE id = ?";
        String insertTxSql = """
            INSERT INTO transactions (product_id, transaction_type, quantity, previous_stock, resulting_stock, transaction_date, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();

            // Step 1: Begin transaction
            conn.setAutoCommit(false);

            int currentStock;
            String sku;
            String name;

            // Step 2: Retrieve current product stock
            try (PreparedStatement psSelect = conn.prepareStatement(selectProductSql)) {
                psSelect.setInt(1, productId);
                try (ResultSet rs = psSelect.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Product with ID " + productId + " does not exist.");
                    }
                    sku = rs.getString("sku");
                    name = rs.getString("name");
                    currentStock = rs.getInt("quantity");
                }
            }

            // Step 3 & 4: Validate requested quantity and calculate resulting stock
            int resultingStock;
            if ("STOCK_IN".equalsIgnoreCase(type)) {
                resultingStock = currentStock + quantity;
            } else if ("STOCK_OUT".equalsIgnoreCase(type)) {
                if (quantity > currentStock) {
                    throw new IllegalArgumentException(
                            "Insufficient stock. Requested " + quantity + " units, but current stock is only " + currentStock + " units."
                    );
                }
                resultingStock = currentStock - quantity;
            } else {
                throw new IllegalArgumentException("Unsupported transaction type: " + type);
            }

            if (resultingStock < 0) {
                throw new IllegalArgumentException("Resulting stock cannot be negative.");
            }

            String now = LocalDateTime.now().format(DATE_TIME_FORMATTER);

            // Step 5: Update product quantity
            try (PreparedStatement psUpdate = conn.prepareStatement(updateProductSql)) {
                psUpdate.setInt(1, resultingStock);
                psUpdate.setString(2, now);
                psUpdate.setInt(3, productId);

                int rowsUpdated = psUpdate.executeUpdate();
                if (rowsUpdated == 0) {
                    throw new SQLException("Failed to update product stock: product row not matched.");
                }
            }

            // Step 6: Insert transaction record
            int generatedId;
            try (PreparedStatement psInsert = conn.prepareStatement(insertTxSql, Statement.RETURN_GENERATED_KEYS)) {
                psInsert.setInt(1, productId);
                psInsert.setString(2, type);
                psInsert.setInt(3, quantity);
                psInsert.setInt(4, currentStock);
                psInsert.setInt(5, resultingStock);
                psInsert.setString(6, now);
                psInsert.setString(7, notes != null ? notes.trim() : "");

                psInsert.executeUpdate();
                try (ResultSet rsKeys = psInsert.getGeneratedKeys()) {
                    if (rsKeys.next()) {
                        generatedId = rsKeys.getInt(1);
                    } else {
                        throw new SQLException("Failed to retrieve generated transaction ID.");
                    }
                }
            }

            // Step 7: Commit transaction
            conn.commit();

            return new Transaction(
                    generatedId,
                    productId,
                    sku,
                    name,
                    type,
                    quantity,
                    currentStock,
                    resultingStock,
                    now,
                    notes
            );

        } catch (Exception e) {
            // Roll back on any error
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Transaction rollback failed: " + rollbackEx.getMessage());
                }
            }
            if (e instanceof IllegalArgumentException iae) {
                throw iae;
            } else if (e instanceof SQLException sqle) {
                throw sqle;
            } else {
                throw new SQLException("Inventory transaction failed: " + e.getMessage(), e);
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    // safe close
                }
            }
        }
    }

    public List<Transaction> getAllTransactions() {
        return transactionDao.findAll();
    }

    public List<Transaction> searchAndFilter(String query, String type, String datePreset) {
        return transactionDao.searchAndFilter(query, type, datePreset);
    }

    public List<Transaction> getRecentTransactions(int limit) {
        return transactionDao.findRecent(limit);
    }

    public List<DailyMovement> get7DayMovementSummary() {
        return transactionDao.fetch7DayMovementSummary();
    }
}
