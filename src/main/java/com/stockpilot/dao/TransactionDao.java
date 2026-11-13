package com.stockpilot.dao;

import com.stockpilot.database.DatabaseManager;
import com.stockpilot.model.DailyMovement;
import com.stockpilot.model.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data Access Object providing SQLite persistence and querying for inventory transactions.
 */
public class TransactionDao {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd");

    public TransactionDao() {
        DatabaseManager.initializeDatabase();
    }

    /**
     * Checks if any historical transaction exists for a given product ID.
     * Used to prevent deletion of products that have audit history.
     */
    public boolean hasTransactionsForProduct(int productId) {
        String sql = "SELECT COUNT(*) FROM transactions WHERE product_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking transactions for product " + productId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves all transactions joined with product SKU and name.
     */
    public List<Transaction> findAll() {
        List<Transaction> list = new ArrayList<>();
        String sql = """
            SELECT t.id, t.product_id, p.sku, p.name, t.transaction_type, t.quantity,
                   t.previous_stock, t.resulting_stock, t.transaction_date, t.notes
            FROM transactions t
            JOIN products p ON t.product_id = p.id
            ORDER BY t.id DESC
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving all transactions: " + e.getMessage());
        }
        return list;
    }

    /**
     * Finds a single transaction by primary key ID.
     */
    public Optional<Transaction> findById(int id) {
        String sql = """
            SELECT t.id, t.product_id, p.sku, p.name, t.transaction_type, t.quantity,
                   t.previous_stock, t.resulting_stock, t.transaction_date, t.notes
            FROM transactions t
            JOIN products p ON t.product_id = p.id
            WHERE t.id = ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding transaction by id: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Searches and filters transactions by text query, transaction type, and date preset.
     */
    public List<Transaction> searchAndFilter(String query, String type, String datePreset) {
        List<Transaction> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT t.id, t.product_id, p.sku, p.name, t.transaction_type, t.quantity,
                   t.previous_stock, t.resulting_stock, t.transaction_date, t.notes
            FROM transactions t
            JOIN products p ON t.product_id = p.id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            String wildcard = "%" + query.trim().toLowerCase() + "%";
            sql.append(" AND (LOWER(p.sku) LIKE ? OR LOWER(p.name) LIKE ? OR LOWER(t.notes) LIKE ?)");
            params.add(wildcard);
            params.add(wildcard);
            params.add(wildcard);
        }

        if (type != null && !type.isBlank() && !type.equalsIgnoreCase("All Types")) {
            String dbType = type.contains("In") ? "STOCK_IN" : "STOCK_OUT";
            sql.append(" AND t.transaction_type = ?");
            params.add(dbType);
        }

        if (datePreset != null && !datePreset.isBlank() && !datePreset.equalsIgnoreCase("All Time")) {
            LocalDate today = LocalDate.now();
            switch (datePreset) {
                case "Today" -> {
                    sql.append(" AND DATE(t.transaction_date) = ?");
                    params.add(today.format(DATE_FORMATTER));
                }
                case "Last 7 Days" -> {
                    sql.append(" AND DATE(t.transaction_date) >= ?");
                    params.add(today.minusDays(6).format(DATE_FORMATTER));
                }
                case "Last 30 Days" -> {
                    sql.append(" AND DATE(t.transaction_date) >= ?");
                    params.add(today.minusDays(29).format(DATE_FORMATTER));
                }
            }
        }

        sql.append(" ORDER BY t.id DESC");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error filtering transactions: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves the N most recent transaction records for the dashboard activity feed.
     */
    public List<Transaction> findRecent(int limit) {
        List<Transaction> list = new ArrayList<>();
        String sql = """
            SELECT t.id, t.product_id, p.sku, p.name, t.transaction_type, t.quantity,
                   t.previous_stock, t.resulting_stock, t.transaction_date, t.notes
            FROM transactions t
            JOIN products p ON t.product_id = p.id
            ORDER BY t.id DESC
            LIMIT ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving recent transactions: " + e.getMessage());
        }
        return list;
    }

    /**
     * Calculates actual 7-day Stock In and Stock Out totals grouped by day directly from SQLite.
     * Populates all 7 days in sequence so the LineChart renders a continuous timeline.
     */
    public List<DailyMovement> fetch7DayMovementSummary() {
        LocalDate today = LocalDate.now();
        LocalDate startDay = today.minusDays(6);

        // Pre-populate 7-day buckets
        Map<String, int[]> dayBuckets = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = startDay.plusDays(i);
            dayBuckets.put(d.format(DATE_FORMATTER), new int[]{0, 0}); // [stockIn, stockOut]
        }

        String sql = """
            SELECT DATE(transaction_date) as t_date,
                   SUM(CASE WHEN transaction_type = 'STOCK_IN' THEN quantity ELSE 0 END) as stock_in,
                   SUM(CASE WHEN transaction_type = 'STOCK_OUT' THEN quantity ELSE 0 END) as stock_out
            FROM transactions
            WHERE DATE(transaction_date) >= ?
            GROUP BY DATE(transaction_date)
            ORDER BY t_date ASC
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, startDay.format(DATE_FORMATTER));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String dateKey = rs.getString("t_date");
                    if (dayBuckets.containsKey(dateKey)) {
                        dayBuckets.get(dateKey)[0] = rs.getInt("stock_in");
                        dayBuckets.get(dateKey)[1] = rs.getInt("stock_out");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching 7-day movement summary: " + e.getMessage());
        }

        List<DailyMovement> movements = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : dayBuckets.entrySet()) {
            LocalDate d = LocalDate.parse(entry.getKey(), DATE_FORMATTER);
            String displayLabel = d.format(DISPLAY_DATE_FORMATTER);
            movements.add(new DailyMovement(displayLabel, entry.getValue()[0], entry.getValue()[1]));
        }
        return movements;
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getInt("id"),
                rs.getInt("product_id"),
                rs.getString("sku"),
                rs.getString("name"),
                rs.getString("transaction_type"),
                rs.getInt("quantity"),
                rs.getInt("previous_stock"),
                rs.getInt("resulting_stock"),
                rs.getString("transaction_date"),
                rs.getString("notes")
        );
    }
}

