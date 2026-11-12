package com.stockpilot.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Manages SQLite database connectivity, schema initialization, table creation,
 * foreign key enforcement, schema migration, and seed records.
 */
public final class DatabaseManager {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static boolean initialized = false;

    private DatabaseManager() {
        // Utility class; do not instantiate.
    }

    /**
     * Obtains a new SQLite connection with foreign key constraints enabled.
     *
     * @return active SQLite Connection
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DatabaseConfig.getJdbcUrl());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
        }
        return connection;
    }

    /**
     * Initializes database tables, executes schema migrations, and loads seed data.
     * Safe to call multiple times.
     */
    public static synchronized void initializeDatabase() {
        if (initialized) {
            return;
        }

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            // 1. Categories table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE
                );
            """);

            // 2. Suppliers table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS suppliers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    email TEXT,
                    phone TEXT,
                    address TEXT
                );
            """);

            // 3. Products table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sku TEXT NOT NULL UNIQUE,
                    name TEXT NOT NULL,
                    description TEXT,
                    category TEXT NOT NULL,
                    quantity INTEGER NOT NULL DEFAULT 0,
                    minimum_stock INTEGER NOT NULL DEFAULT 0,
                    unit_price NUMERIC NOT NULL DEFAULT 0.00,
                    supplier_id INTEGER,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL
                );
            """);

            // 4. Safe migration for Transactions table (enforcing ON DELETE RESTRICT and stock auditing)
            migrateTransactionsTable(connection);

            // Seed initial lookup data if empty
            seedInitialCategories(connection);
            seedInitialSuppliers(connection);
            seedInitialProducts(connection);
            seedInitialTransactions(connection);

            initialized = true;
            System.out.println("SQLite database successfully initialized at: " + DatabaseConfig.getDatabasePath().toAbsolutePath());

        } catch (SQLException e) {
            System.err.println("Fatal Error: Failed to initialize SQLite database: " + e.getMessage());
            throw new RuntimeException("Database initialization failure", e);
        }
    }

    /**
     * Ensures the transactions table exists with previous_stock and resulting_stock columns,
     * and guarantees that foreign key deletion constraint is ON DELETE RESTRICT to protect audit history.
     */
    private static void migrateTransactionsTable(Connection connection) throws SQLException {
        boolean tableExists = false;
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='transactions'")) {
            tableExists = rs.next();
        }

        if (!tableExists) {
            try (Statement st = connection.createStatement()) {
                st.execute("""
                    CREATE TABLE IF NOT EXISTS transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        product_id INTEGER NOT NULL,
                        transaction_type TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        previous_stock INTEGER NOT NULL DEFAULT 0,
                        resulting_stock INTEGER NOT NULL DEFAULT 0,
                        transaction_date TEXT NOT NULL,
                        notes TEXT,
                        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
                    );
                """);
            }
            return;
        }

        // Inspect existing schema
        boolean hasPrevStock = false;
        boolean hasResultStock = false;
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(transactions)")) {
            while (rs.next()) {
                String col = rs.getString("name");
                if ("previous_stock".equalsIgnoreCase(col)) hasPrevStock = true;
                if ("resulting_stock".equalsIgnoreCase(col)) hasResultStock = true;
            }
        }

        boolean isCascade = false;
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA foreign_key_list(transactions)")) {
            while (rs.next()) {
                String onDelete = rs.getString("on_delete");
                if ("CASCADE".equalsIgnoreCase(onDelete)) {
                    isCascade = true;
                }
            }
        }

        // If schema needs upgrade, execute safe SQLite table migration
        if (isCascade || !hasPrevStock || !hasResultStock) {
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = OFF;");
                st.execute("""
                    CREATE TABLE IF NOT EXISTS transactions_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        product_id INTEGER NOT NULL,
                        transaction_type TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        previous_stock INTEGER NOT NULL DEFAULT 0,
                        resulting_stock INTEGER NOT NULL DEFAULT 0,
                        transaction_date TEXT NOT NULL,
                        notes TEXT,
                        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
                    );
                """);

                if (hasPrevStock && hasResultStock) {
                    st.execute("""
                        INSERT INTO transactions_new (id, product_id, transaction_type, quantity, previous_stock, resulting_stock, transaction_date, notes)
                        SELECT id, product_id, transaction_type, quantity, previous_stock, resulting_stock, transaction_date, notes FROM transactions;
                    """);
                } else {
                    st.execute("""
                        INSERT INTO transactions_new (id, product_id, transaction_type, quantity, previous_stock, resulting_stock, transaction_date, notes)
                        SELECT id, product_id, transaction_type, quantity, 0, quantity, transaction_date, notes FROM transactions;
                    """);
                }

                st.execute("DROP TABLE transactions;");
                st.execute("ALTER TABLE transactions_new RENAME TO transactions;");
                st.execute("PRAGMA foreign_keys = ON;");
            }
        }
    }

    private static void seedInitialCategories(Connection connection) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM categories";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insertSql = "INSERT INTO categories (name) VALUES (?)";
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    String[] categories = {"Peripherals", "Networking", "Displays", "Storage", "Cables & Power"};
                    for (String cat : categories) {
                        ps.setString(1, cat);
                        ps.executeUpdate();
                    }
                }
            }
        }
    }

    private static void seedInitialSuppliers(Connection connection) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM suppliers";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insertSql = "INSERT INTO suppliers (name, email, phone, address) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    ps.setString(1, "Apex Logistics Components");
                    ps.setString(2, "orders@apexcomponents.com");
                    ps.setString(3, "+1 (555) 234-5678");
                    ps.setString(4, "742 Evergreen Industrial Pkwy, Sector 4");
                    ps.executeUpdate();

                    ps.setString(1, "Nexus Hardware Group");
                    ps.setString(2, "supply@nexushardware.org");
                    ps.setString(3, "+1 (555) 876-5432");
                    ps.setString(4, "120 Silicon Boulevard, Warehouse Bay 12");
                    ps.executeUpdate();
                }
            }
        }
    }

    private static void seedInitialProducts(Connection connection) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM products";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String now = LocalDateTime.now().format(DATE_TIME_FORMATTER);
                String insertSql = """
                    INSERT INTO products (sku, name, description, category, quantity, minimum_stock, unit_price, supplier_id, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    insertProductSeed(ps, "PRD-PER-001", "USB-C Docking Station", "Triple 4K display dock with 100W PD", "Peripherals", 45, 10, 149.99, 1, now);
                    insertProductSeed(ps, "PRD-PER-002", "Ergonomic Wireless Mouse", "Dual Bluetooth and 2.4GHz precision mouse", "Peripherals", 18, 10, 49.50, 1, now);
                    insertProductSeed(ps, "PRD-PER-003", "Mechanical Keyboard (MX Brown)", "Tenkeyless tactical mechanical keyboard", "Peripherals", 24, 8, 89.99, 1, now);
                    insertProductSeed(ps, "PRD-NET-002", "Gigabit Managed Switch 16-Port", "Layer 2 managed rackmount PoE+ switch", "Networking", 14, 5, 219.00, 2, now);
                    insertProductSeed(ps, "PRD-STO-001", "2TB NVMe M.2 Solid State Drive", "PCIe 4.0 high throughput internal SSD", "Storage", 35, 10, 139.99, 2, now);
                    insertProductSeed(ps, "PRD-STO-002", "External Rugged HDD 4TB", "Shockproof USB 3.2 mobile backup drive", "Storage", 12, 8, 109.50, 2, now);
                    insertProductSeed(ps, "PRD-CAB-001", "Thunderbolt 4 Cable (2m)", "40Gbps certified high-throughput cable", "Cables & Power", 50, 15, 29.99, 1, now);
                    insertProductSeed(ps, "PRD-DIS-002", "Adjustable Dual Monitor Arm", "Gas spring articulating desk mount", "Displays", 16, 5, 79.99, 2, now);
                    insertProductSeed(ps, "PRD-DIS-001", "27\" 4K IPS Color Monitor", "Color-calibrated UHD monitor 99% sRGB", "Displays", 6, 10, 329.00, 1, now);
                    insertProductSeed(ps, "PRD-NET-001", "Cat6 Shielded Patch Cable (5m)", "Snagless RJ45 10Gbps patch cord", "Cables & Power", 0, 20, 12.50, 1, now);
                }
            }
        }
    }

    private static void seedInitialTransactions(Connection connection) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM transactions";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                // Find products to associate realistic initial transaction history
                Integer p1 = findProductIdBySku(connection, "PRD-PER-001");
                Integer p2 = findProductIdBySku(connection, "PRD-PER-002");
                Integer p3 = findProductIdBySku(connection, "PRD-DIS-001");
                Integer p4 = findProductIdBySku(connection, "PRD-PER-003");
                Integer p5 = findProductIdBySku(connection, "PRD-STO-001");

                String insertSql = """
                    INSERT INTO transactions (product_id, transaction_type, quantity, previous_stock, resulting_stock, transaction_date, notes)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    LocalDateTime now = LocalDateTime.now();

                    if (p1 != null) {
                        insertTransactionSeed(ps, p1, "STOCK_IN", 45, 0, 45, now.minusDays(5).format(DATE_TIME_FORMATTER), "Initial pallet receiving intake");
                    }
                    if (p2 != null) {
                        insertTransactionSeed(ps, p2, "STOCK_IN", 28, 0, 28, now.minusDays(4).format(DATE_TIME_FORMATTER), "Inbound vendor shipment #104");
                        insertTransactionSeed(ps, p2, "STOCK_OUT", 10, 28, 18, now.minusDays(2).format(DATE_TIME_FORMATTER), "Dispatched to Regional Hub B");
                    }
                    if (p3 != null) {
                        insertTransactionSeed(ps, p3, "STOCK_IN", 10, 0, 10, now.minusDays(3).format(DATE_TIME_FORMATTER), "Inbound Apex shipment");
                        insertTransactionSeed(ps, p3, "STOCK_OUT", 4, 10, 6, now.minusDays(1).format(DATE_TIME_FORMATTER), "Customer order fulfillment #10842");
                    }
                    if (p4 != null) {
                        insertTransactionSeed(ps, p4, "STOCK_IN", 30, 0, 30, now.minusDays(2).format(DATE_TIME_FORMATTER), "Inbound keyboard stock");
                        insertTransactionSeed(ps, p4, "STOCK_OUT", 6, 30, 24, now.minusDays(1).format(DATE_TIME_FORMATTER), "Fulfillment dispatch #10855");
                    }
                    if (p5 != null) {
                        insertTransactionSeed(ps, p5, "STOCK_IN", 35, 0, 35, now.format(DATE_TIME_FORMATTER), "Restock pallet intake dock");
                    }
                }
            }
        }
    }

    private static Integer findProductIdBySku(Connection conn, String sku) throws SQLException {
        String sql = "SELECT id FROM products WHERE sku = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return null;
    }

    private static void insertProductSeed(PreparedStatement ps, String sku, String name, String desc,
                                          String cat, int qty, int minStock, double price, int supplierId,
                                          String now) throws SQLException {
        ps.setString(1, sku);
        ps.setString(2, name);
        ps.setString(3, desc);
        ps.setString(4, cat);
        ps.setInt(5, qty);
        ps.setInt(6, minStock);
        ps.setDouble(7, price);
        ps.setInt(8, supplierId);
        ps.setString(9, now);
        ps.setString(10, now);
        ps.executeUpdate();
    }

    private static void insertTransactionSeed(PreparedStatement ps, int productId, String type,
                                              int qty, int prevStock, int resultStock,
                                              String date, String notes) throws SQLException {
        ps.setInt(1, productId);
        ps.setString(2, type);
        ps.setInt(3, qty);
        ps.setInt(4, prevStock);
        ps.setInt(5, resultStock);
        ps.setString(6, date);
        ps.setString(7, notes);
        ps.executeUpdate();
    }
}
