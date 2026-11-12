package com.stockpilot;

import com.stockpilot.database.DatabaseConfig;
import com.stockpilot.database.DatabaseManager;
import com.stockpilot.model.ActivityItem;
import com.stockpilot.model.DailyMovement;
import com.stockpilot.model.DashboardSummary;
import com.stockpilot.model.Product;
import com.stockpilot.model.Transaction;
import com.stockpilot.service.DashboardService;
import com.stockpilot.service.InventoryService;
import com.stockpilot.service.ProductService;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Automated test suite verifying the Inventory Transactions milestone:
 * - Atomic Stock In & Stock Out operations with JDBC transaction commit/rollback
 * - Insufficient stock protection (never negative stock)
 * - Zero and negative quantity rejection
 * - Accurate previous_stock and resulting_stock calculation and persistence
 * - Audit ledger preservation (blocking deletion of products with transaction history)
 * - Transaction filtering, searching, and date presets
 * - Live Dashboard integration (stock velocity line chart, recent activity feed)
 */
public class VerifyInventoryTransactions {

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("Starting StockPilot Inventory Transactions Automated Verification");
        System.out.println("======================================================================");

        try {
            // 1. Database Initialization
            DatabaseManager.initializeDatabase();
            if (!Files.exists(DatabaseConfig.getDatabasePath())) {
                throw new AssertionError("Database file does not exist at: " + DatabaseConfig.getDatabasePath());
            }
            System.out.println("[PASS] SQLite database initialized and verified.");

            ProductService productService = new ProductService();
            InventoryService inventoryService = new InventoryService();
            DashboardService dashboardService = new DashboardService();

            // 2. Setup a fresh test product
            String testSku = "TX-TEST-" + (System.currentTimeMillis() % 100000);
            Product initialProduct = new Product(
                    testSku,
                    "Industrial Pneumatic Actuator",
                    "Precision actuator for automated assembly test",
                    "Hardware",
                    20, // initial stock
                    5,  // min stock
                    new BigDecimal("145.00")
            );
            Product savedProduct = productService.saveProduct(initialProduct);
            int productId = savedProduct.getId();
            System.out.println("[PASS] Test product created: ID=" + productId + " [SKU=" + testSku + "] Initial Qty=20");

            // 3. Test Stock In Operation
            int stockInQty = 15;
            Transaction txIn = inventoryService.recordStockIn(productId, stockInQty, "Inbound shipment PO-8841");
            if (txIn == null || txIn.getId() <= 0) {
                throw new AssertionError("Stock In did not return a valid persisted transaction!");
            }
            if (!"STOCK_IN".equals(txIn.getTransactionType())) {
                throw new AssertionError("Expected transaction type 'STOCK_IN', got: " + txIn.getTransactionType());
            }
            if (txIn.getPreviousStock() != 20 || txIn.getResultingStock() != 35 || txIn.getQuantity() != 15) {
                throw new AssertionError("Stock In ledger math error: prev=" + txIn.getPreviousStock()
                        + ", resulting=" + txIn.getResultingStock() + " (expected 20 -> 35)");
            }

            // Verify product quantity in database
            Product afterStockIn = productService.getProductById(productId).orElseThrow();
            if (afterStockIn.getQuantity() != 35) {
                throw new AssertionError("Product quantity was not updated to 35 in database! Found: " + afterStockIn.getQuantity());
            }
            System.out.println("[PASS] Stock In verified: 20 + 15 = 35. Tx ID: " + txIn.getId() + ", Resulting Stock: " + txIn.getResultingStock());

            // 4. Test Stock Out Operation
            int stockOutQty = 10;
            Transaction txOut = inventoryService.recordStockOut(productId, stockOutQty, "Dispatched for line assembly WO-102");
            if (!"STOCK_OUT".equals(txOut.getTransactionType())) {
                throw new AssertionError("Expected transaction type 'STOCK_OUT', got: " + txOut.getTransactionType());
            }
            if (txOut.getPreviousStock() != 35 || txOut.getResultingStock() != 25 || txOut.getQuantity() != 10) {
                throw new AssertionError("Stock Out ledger math error: prev=" + txOut.getPreviousStock()
                        + ", resulting=" + txOut.getResultingStock() + " (expected 35 -> 25)");
            }

            Product afterStockOut = productService.getProductById(productId).orElseThrow();
            if (afterStockOut.getQuantity() != 25) {
                throw new AssertionError("Product quantity was not updated to 25 in database! Found: " + afterStockOut.getQuantity());
            }
            System.out.println("[PASS] Stock Out verified: 35 - 10 = 25. Tx ID: " + txOut.getId() + ", Resulting Stock: " + txOut.getResultingStock());

            // 5. Test Stock Out Exceeding Available Stock (Prevent Negative Stock)
            boolean insufficientStockCaught = false;
            try {
                inventoryService.recordStockOut(productId, 50, "Exceeds available stock");
            } catch (IllegalArgumentException e) {
                insufficientStockCaught = true;
                System.out.println("[PASS] Insufficient stock caught as expected: " + e.getMessage());
            }
            if (!insufficientStockCaught) {
                throw new AssertionError("Stock Out should have been rejected for quantity (50) exceeding stock (25)!");
            }

            // Verify product quantity is STILL 25 (rollback verified)
            Product afterFailedStockOut = productService.getProductById(productId).orElseThrow();
            if (afterFailedStockOut.getQuantity() != 25) {
                throw new AssertionError("Stock was altered after failed transaction! Rollback failed. Found: " + afterFailedStockOut.getQuantity());
            }
            System.out.println("[PASS] Atomic rollback verified: stock remained unchanged at 25 after failed transaction.");

            // 6. Test Rejection of Zero and Negative Quantities
            boolean zeroInCaught = false;
            try {
                inventoryService.recordStockIn(productId, 0, "Zero qty in");
            } catch (IllegalArgumentException e) {
                zeroInCaught = true;
            }
            if (!zeroInCaught) throw new AssertionError("Zero Stock In quantity was not rejected!");

            boolean negativeInCaught = false;
            try {
                inventoryService.recordStockIn(productId, -5, "Negative qty in");
            } catch (IllegalArgumentException e) {
                negativeInCaught = true;
            }
            if (!negativeInCaught) throw new AssertionError("Negative Stock In quantity was not rejected!");

            boolean zeroOutCaught = false;
            try {
                inventoryService.recordStockOut(productId, 0, "Zero qty out");
            } catch (IllegalArgumentException e) {
                zeroOutCaught = true;
            }
            if (!zeroOutCaught) throw new AssertionError("Zero Stock Out quantity was not rejected!");

            boolean negativeOutCaught = false;
            try {
                inventoryService.recordStockOut(productId, -12, "Negative qty out");
            } catch (IllegalArgumentException e) {
                negativeOutCaught = true;
            }
            if (!negativeOutCaught) throw new AssertionError("Negative Stock Out quantity was not rejected!");
            System.out.println("[PASS] Zero and negative transaction quantities strictly rejected.");

            // 7. Audit History Preservation (Prevent Product Deletion)
            boolean deletionBlocked = false;
            try {
                productService.deleteProduct(productId);
            } catch (IllegalStateException e) {
                deletionBlocked = true;
                System.out.println("[PASS] Product deletion blocked to preserve audit trail: " + e.getMessage());
            }
            if (!deletionBlocked) {
                throw new AssertionError("Product deletion was NOT blocked despite having historical transactions!");
            }

            // Verify product is still present
            Optional<Product> preservedProduct = productService.getProductById(productId);
            if (preservedProduct.isEmpty()) {
                throw new AssertionError("Product was deleted despite having audit history!");
            }
            System.out.println("[PASS] Audit preservation verified: Product " + testSku + " preserved in database.");

            // 8. Test Search and Filtering
            List<Transaction> filteredBySku = inventoryService.searchAndFilter(testSku, "All Types", "All Time");
            if (filteredBySku.size() < 2) {
                throw new AssertionError("Expected at least 2 transactions for " + testSku + ", found: " + filteredBySku.size());
            }
            System.out.println("[PASS] Search by SKU verified: Found " + filteredBySku.size() + " transactions for " + testSku);

            List<Transaction> filteredByTypeIn = inventoryService.searchAndFilter(testSku, "Stock In", "All Time");
            for (Transaction t : filteredByTypeIn) {
                if (!"STOCK_IN".equals(t.getTransactionType())) {
                    throw new AssertionError("Filter by type 'Stock In' returned non-in transaction: " + t.getTransactionType());
                }
            }
            System.out.println("[PASS] Filter by Type verified: " + filteredByTypeIn.size() + " STOCK_IN records.");

            List<Transaction> filteredByDateToday = inventoryService.searchAndFilter(testSku, "All Types", "Today");
            if (filteredByDateToday.isEmpty()) {
                throw new AssertionError("Filter by date 'Today' did not return transactions created today!");
            }
            System.out.println("[PASS] Filter by Date preset ('Today') verified.");

            // 9. Dashboard Integration Verification
            DashboardSummary summary = dashboardService.getDashboardSummary();
            if (summary.getTotalProducts() <= 0 || summary.getTotalStockUnits() <= 0) {
                throw new AssertionError("Dashboard metrics invalid: totalProducts=" + summary.getTotalProducts()
                        + ", totalStockUnits=" + summary.getTotalStockUnits());
            }
            System.out.println("[PASS] Live Dashboard Metrics: Products=" + summary.getTotalProducts()
                    + ", Stock Units=" + summary.getTotalStockUnits()
                    + ", Low Stock=" + summary.getLowStockItems());

            List<DailyMovement> movements = dashboardService.getStockMovements();
            if (movements.isEmpty()) {
                throw new AssertionError("Dashboard 7-day movement velocity chart data is empty!");
            }
            DailyMovement todayMovement = movements.get(movements.size() - 1);
            System.out.println("[PASS] Dashboard 7-Day Velocity Chart verified: Today Stock In="
                    + todayMovement.getStockInUnits() + ", Stock Out=" + todayMovement.getStockOutUnits());

            List<ActivityItem> recentActivity = dashboardService.getRecentActivity();
            if (recentActivity.isEmpty()) {
                throw new AssertionError("Dashboard recent activity list is empty!");
            }
            System.out.println("[PASS] Dashboard Recent Activity Feed verified (" + recentActivity.size() + " entries). Top item: "
                    + recentActivity.get(0).getItemName() + " (" + recentActivity.get(0).getAction() + ")");

            // 10. Clean Product Deletion Verification (for products without transactions)
            String cleanSku = "CLEAN-" + (System.currentTimeMillis() % 100000);
            Product cleanProduct = new Product(cleanSku, "Temporary Item", "No tx", "Electronics", 5, 2, new BigDecimal("10.00"));
            Product savedClean = productService.saveProduct(cleanProduct);
            boolean cleanDeleted = productService.deleteProduct(savedClean.getId());
            if (!cleanDeleted || productService.getProductById(savedClean.getId()).isPresent()) {
                throw new AssertionError("Product without transactions should be deletable!");
            }
            System.out.println("[PASS] Clean deletion verified for un-transacted product (ID: " + savedClean.getId() + ")");

            System.out.println("======================================================================");
            System.out.println("ALL INVENTORY TRANSACTION TESTS PASSED SUCCESSFULLY! (100% VERIFIED)");
            System.out.println("======================================================================");

        } catch (Exception e) {
            System.err.println("[FAIL] Test verification failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
