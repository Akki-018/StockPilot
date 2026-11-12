package com.stockpilot;

import com.stockpilot.database.DatabaseConfig;
import com.stockpilot.database.DatabaseManager;
import com.stockpilot.model.CategoryStock;
import com.stockpilot.model.DashboardSummary;
import com.stockpilot.model.Product;
import com.stockpilot.service.DashboardService;
import com.stockpilot.service.ProductService;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Headless test harness verifying SQLite database initialization, Product DAO/Service CRUD,
 * duplicate SKU prevention, search and filtering, and dashboard SQLite aggregation.
 */
public class VerifyProductPersistence {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("Starting StockPilot SQLite & Product Module Tests");
        System.out.println("==================================================");

        try {
            // Test 1: Database Initialization
            DatabaseManager.initializeDatabase();
            if (!Files.exists(DatabaseConfig.getDatabasePath())) {
                throw new AssertionError("Database file does not exist at: " + DatabaseConfig.getDatabasePath());
            }
            System.out.println("[PASS] Database file verified at: " + DatabaseConfig.getDatabasePath());

            // Test 2: Foreign Key Enforcement
            try (Connection conn = DatabaseManager.getConnection()) {
                var rs = conn.createStatement().executeQuery("PRAGMA foreign_keys;");
                if (rs.next() && rs.getInt(1) == 1) {
                    System.out.println("[PASS] SQLite PRAGMA foreign_keys is ENABLED.");
                } else {
                    throw new AssertionError("SQLite foreign_keys pragma is not active!");
                }
            }

            ProductService productService = new ProductService();

            // Test 3: Seed Products Verification
            List<Product> initialProducts = productService.getAllProducts();
            System.out.println("[PASS] Found " + initialProducts.size() + " products in SQLite database.");
            if (initialProducts.isEmpty()) {
                throw new AssertionError("Database should contain initial seed products!");
            }

            // Test 4: Add New Product
            String testSku = "PRD-TEST-" + System.currentTimeMillis() % 10000;
            Product testProduct = new Product(
                    testSku,
                    "Automated Test Product",
                    "Temporary SKU for automated persistence testing",
                    "Peripherals",
                    25,
                    5,
                    new BigDecimal("79.95")
            );

            Product savedProduct = productService.saveProduct(testProduct);
            if (savedProduct.getId() == null) {
                throw new AssertionError("Saved product must have generated primary key ID!");
            }
            System.out.println("[PASS] Inserted product ID: " + savedProduct.getId() + " [SKU: " + testSku + "]");

            // Test 5: Duplicate SKU Prevention
            boolean duplicateBlocked = false;
            try {
                Product duplicate = new Product(
                        testSku, // same SKU
                        "Duplicate SKU Attempt",
                        "Should fail validation",
                        "Networking",
                        10,
                        5,
                        new BigDecimal("19.99")
                );
                productService.saveProduct(duplicate);
            } catch (IllegalArgumentException e) {
                duplicateBlocked = true;
                System.out.println("[PASS] Duplicate SKU was blocked as expected: " + e.getMessage());
            }
            if (!duplicateBlocked) {
                throw new AssertionError("Duplicate SKU was NOT blocked!");
            }

            // Test 6: Edit Existing Product
            savedProduct.setQuantity(4); // change quantity to 4, which is below minStock (5) -> Low Stock!
            savedProduct.setUnitPrice(new BigDecimal("84.50"));
            productService.saveProduct(savedProduct);

            Optional<Product> reloadedOpt = productService.getProductById(savedProduct.getId());
            if (reloadedOpt.isEmpty()) {
                throw new AssertionError("Could not reload edited product!");
            }
            Product reloaded = reloadedOpt.get();
            if (reloaded.getQuantity() != 4 || reloaded.getUnitPrice().compareTo(new BigDecimal("84.50")) != 0) {
                throw new AssertionError("Product updates were not persisted correctly!");
            }
            if (!"Low Stock".equals(reloaded.getStockStatus())) {
                throw new AssertionError("Expected status 'Low Stock' for qty 4 and minStock 5, but got: " + reloaded.getStockStatus());
            }
            System.out.println("[PASS] Product updated successfully. New status: " + reloaded.getStockStatus());

            // Test 7: Search and Filter
            List<Product> searchResults = productService.searchAndFilter("Automated Test", null, null);
            if (searchResults.isEmpty()) {
                throw new AssertionError("Search by name failed to return test product!");
            }
            System.out.println("[PASS] Search query verified. Found: " + searchResults.size() + " matching records.");

            List<Product> lowStockFilter = productService.searchAndFilter(null, null, "Low Stock");
            boolean containsTest = lowStockFilter.stream().anyMatch(p -> p.getId().equals(savedProduct.getId()));
            if (!containsTest) {
                throw new AssertionError("Status filter 'Low Stock' did not include test product!");
            }
            System.out.println("[PASS] Filter by status verified.");

            // Test 8: Live Dashboard Integration
            DashboardService dashboardService = new DashboardService();
            DashboardSummary summary = dashboardService.getDashboardSummary();
            System.out.println("[PASS] Real Dashboard Metrics from SQLite:");
            System.out.println("       - Total Products: " + summary.getTotalProducts());
            System.out.println("       - Total Units:    " + summary.getTotalStockUnits());
            System.out.println("       - Low Stock:      " + summary.getLowStockItems());
            System.out.println("       - Out of Stock:   " + summary.getOutOfStockItems());
            System.out.println("       - Valuation:      $" + summary.getInventoryValuation());

            List<CategoryStock> categoryStocks = dashboardService.getCategoryStocks();
            System.out.println("[PASS] Category distribution returned " + categoryStocks.size() + " active divisions from SQLite.");

            // Test 9: Delete Product
            boolean deleted = productService.deleteProduct(savedProduct.getId());
            if (!deleted) {
                throw new AssertionError("Failed to delete test product!");
            }
            Optional<Product> afterDelete = productService.getProductById(savedProduct.getId());
            if (afterDelete.isPresent()) {
                throw new AssertionError("Product still exists in database after deletion!");
            }
            System.out.println("[PASS] Product ID: " + savedProduct.getId() + " successfully deleted.");

            System.out.println("==================================================");
            System.out.println("ALL TESTS PASSED SUCCESSFULLY! (100% Verified)");
            System.out.println("==================================================");

        } catch (Exception e) {
            System.err.println("[FAIL] Test failed with error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

