package com.stockpilot.dao;

import com.stockpilot.database.DatabaseManager;
import com.stockpilot.model.CategoryStock;
import com.stockpilot.model.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object providing SQLite persistence for Products and inventory queries.
 */
public class ProductDao {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ProductDao() {
        DatabaseManager.initializeDatabase();
    }

    /**
     * Retrieves all products ordered by latest created.
     */
    public List<Product> findAll() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products ORDER BY id DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                products.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error reading all products: " + e.getMessage());
        }
        return products;
    }

    /**
     * Finds a single product by primary key id.
     */
    public Optional<Product> findById(int id) {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding product by id: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Finds a product by SKU code.
     */
    public Optional<Product> findBySku(String sku) {
        String sql = "SELECT * FROM products WHERE UPPER(sku) = UPPER(?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sku != null ? sku.trim() : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding product by sku: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Checks if a SKU already exists, optionally excluding a specific product id (for edits).
     */
    public boolean existsBySku(String sku, Integer excludeId) {
        if (sku == null || sku.isBlank()) {
            return false;
        }

        String sql = excludeId == null
                ? "SELECT COUNT(*) FROM products WHERE UPPER(sku) = UPPER(?)"
                : "SELECT COUNT(*) FROM products WHERE UPPER(sku) = UPPER(?) AND id <> ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sku.trim());
            if (excludeId != null) {
                ps.setInt(2, excludeId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking SKU existence: " + e.getMessage());
            return false;
        }
    }

    /**
     * Searches and filters products by search query, category, and health status.
     */
    public List<Product> searchAndFilter(String query, String category, String status) {
        List<Product> products = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM products WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            String wildcard = "%" + query.trim().toLowerCase() + "%";
            sql.append(" AND (LOWER(name) LIKE ? OR LOWER(sku) LIKE ?)");
            params.add(wildcard);
            params.add(wildcard);
        }

        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("All Categories")) {
            sql.append(" AND category = ?");
            params.add(category.trim());
        }

        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("All Statuses")) {
            switch (status) {
                case "Healthy" -> sql.append(" AND quantity > minimum_stock");
                case "Low Stock" -> sql.append(" AND quantity > 0 AND quantity <= minimum_stock");
                case "Out of Stock" -> sql.append(" AND quantity <= 0");
            }
        }

        sql.append(" ORDER BY id DESC");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error executing search and filter: " + e.getMessage());
        }
        return products;
    }

    /**
     * Inserts a new product record into SQLite.
     */
    public Product insert(Product product) throws SQLException {
        String sql = """
            INSERT INTO products (sku, name, description, category, quantity, minimum_stock, unit_price, supplier_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        String now = LocalDateTime.now().format(DATE_TIME_FORMATTER);

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, product.getSku().trim().toUpperCase());
            ps.setString(2, product.getName().trim());
            ps.setString(3, product.getDescription() != null ? product.getDescription().trim() : "");
            ps.setString(4, product.getCategory().trim());
            ps.setInt(5, product.getQuantity());
            ps.setInt(6, product.getMinimumStock());
            ps.setDouble(7, product.getUnitPrice().doubleValue());

            if (product.getSupplierId() != null) {
                ps.setInt(8, product.getSupplierId());
            } else {
                ps.setNull(8, java.sql.Types.INTEGER);
            }

            ps.setString(9, now);
            ps.setString(10, now);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Insert product failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    product.setId(generatedKeys.getInt(1));
                }
            }

            product.setCreatedAt(now);
            product.setUpdatedAt(now);
            return product;
        }
    }

    /**
     * Updates an existing product in SQLite.
     */
    public boolean update(Product product) throws SQLException {
        if (product.getId() == null) {
            return false;
        }

        String sql = """
            UPDATE products
            SET sku = ?, name = ?, description = ?, category = ?, quantity = ?, minimum_stock = ?,
                unit_price = ?, supplier_id = ?, updated_at = ?
            WHERE id = ?
        """;

        String now = LocalDateTime.now().format(DATE_TIME_FORMATTER);

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, product.getSku().trim().toUpperCase());
            ps.setString(2, product.getName().trim());
            ps.setString(3, product.getDescription() != null ? product.getDescription().trim() : "");
            ps.setString(4, product.getCategory().trim());
            ps.setInt(5, product.getQuantity());
            ps.setInt(6, product.getMinimumStock());
            ps.setDouble(7, product.getUnitPrice().doubleValue());

            if (product.getSupplierId() != null) {
                ps.setInt(8, product.getSupplierId());
            } else {
                ps.setNull(8, java.sql.Types.INTEGER);
            }

            ps.setString(9, now);
            ps.setInt(10, product.getId());

            boolean success = ps.executeUpdate() > 0;
            if (success) {
                product.setUpdatedAt(now);
            }
            return success;
        }
    }

    /**
     * Deletes a product by primary key id.
     */
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Fetches all distinct category names from database.
     */
    public List<String> findAllCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT name FROM categories ORDER BY name ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                categories.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            System.err.println("Error reading categories: " + e.getMessage());
        }
        return categories;
    }

    // ==================== Real Analytics & Aggregates for Dashboard ====================

    public int countTotalProducts() {
        String sql = "SELECT COUNT(*) FROM products";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error counting products: " + e.getMessage());
        }
        return 0;
    }

    public int sumTotalUnits() {
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM products";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error summing units: " + e.getMessage());
        }
        return 0;
    }

    public int countLowStockProducts() {
        String sql = "SELECT COUNT(*) FROM products WHERE quantity > 0 AND quantity <= minimum_stock";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error counting low stock products: " + e.getMessage());
        }
        return 0;
    }

    public int countOutOfStockProducts() {
        String sql = "SELECT COUNT(*) FROM products WHERE quantity <= 0";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error counting out of stock products: " + e.getMessage());
        }
        return 0;
    }

    public BigDecimal sumInventoryValuation() {
        String sql = "SELECT COALESCE(SUM(quantity * unit_price), 0) FROM products";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return BigDecimal.valueOf(rs.getDouble(1)).setScale(2, RoundingMode.HALF_UP);
            }
        } catch (SQLException e) {
            System.err.println("Error calculating valuation: " + e.getMessage());
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public List<CategoryStock> fetchCategoryStockSummary() {
        List<CategoryStock> list = new ArrayList<>();
        String sql = """
            SELECT category, COUNT(*) as prod_count, COALESCE(SUM(quantity), 0) as unit_sum
            FROM products
            GROUP BY category
            ORDER BY unit_sum DESC
        """;
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new CategoryStock(
                        rs.getString("category"),
                        rs.getInt("prod_count"),
                        rs.getInt("unit_sum")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching category stock summary: " + e.getMessage());
        }
        return list;
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Integer supplierId = rs.getInt("supplier_id");
        if (rs.wasNull()) {
            supplierId = null;
        }

        return new Product(
                rs.getInt("id"),
                rs.getString("sku"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("category"),
                rs.getInt("quantity"),
                rs.getInt("minimum_stock"),
                BigDecimal.valueOf(rs.getDouble("unit_price")),
                supplierId,
                rs.getString("created_at"),
                rs.getString("updated_at")
        );
    }
}

