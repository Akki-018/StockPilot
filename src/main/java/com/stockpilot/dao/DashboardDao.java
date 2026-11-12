package com.stockpilot.dao;

import com.stockpilot.model.ActivityItem;
import com.stockpilot.model.CategoryStock;
import com.stockpilot.model.DailyMovement;
import com.stockpilot.model.DashboardSummary;
import com.stockpilot.model.InventoryHealth;
import com.stockpilot.model.Transaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access object providing dashboard metrics, category distributions,
 * movement trends, and activity logs.
 * All metrics, charts, and activity feeds query real SQLite database records.
 */
public class DashboardDao {

    private final ProductDao productDao;
    private final TransactionDao transactionDao;

    public DashboardDao() {
        this.productDao = new ProductDao();
        this.transactionDao = new TransactionDao();
    }

    /**
     * Calculates real dashboard metrics directly from SQLite database:
     * - Total distinct products
     * - Total stock units
     * - Low-stock products
     * - Out-of-stock products
     * - Total inventory valuation
     */
    public DashboardSummary fetchSummary() {
        int totalProducts = productDao.countTotalProducts();
        int totalUnits = productDao.sumTotalUnits();
        int lowStock = productDao.countLowStockProducts();
        int outOfStock = productDao.countOutOfStockProducts();
        BigDecimal valuation = productDao.sumInventoryValuation();

        return new DashboardSummary(totalProducts, totalUnits, lowStock, outOfStock, valuation);
    }

    /**
     * Real category distribution and stock levels queried directly from SQLite products table.
     */
    public List<CategoryStock> fetchCategoryStocks() {
        return productDao.fetchCategoryStockSummary();
    }

    /**
     * Real inventory health calculation based on live SQLite product records.
     */
    public InventoryHealth fetchInventoryHealth() {
        int lowStock = productDao.countLowStockProducts();
        int outOfStock = productDao.countOutOfStockProducts();
        int totalProducts = productDao.countTotalProducts();
        int healthy = Math.max(0, totalProducts - lowStock - outOfStock);

        return new InventoryHealth(healthy, lowStock, outOfStock);
    }

    /**
     * Seven-day stock movement history queried directly from SQLite transactions table.
     */
    public List<DailyMovement> fetchStockMovements() {
        return transactionDao.fetch7DayMovementSummary();
    }

    /**
     * Recent inventory activity events queried directly from SQLite transactions table.
     */
    public List<ActivityItem> fetchRecentActivity() {
        List<Transaction> recentTx = transactionDao.findRecent(5);
        List<ActivityItem> activityItems = new ArrayList<>();

        for (Transaction tx : recentTx) {
            String time = tx.getTransactionDate();
            String action = tx.isStockIn() ? "Stock In" : "Stock Out";
            String itemDesc = tx.getProductName() + " (" + tx.getProductSku() + ")";
            String delta = tx.getFormattedDelta();
            String status = "Completed";

            activityItems.add(new ActivityItem(time, action, itemDesc, delta, status));
        }

        return activityItems;
    }
}
