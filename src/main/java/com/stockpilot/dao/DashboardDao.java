package com.stockpilot.dao;

import com.stockpilot.model.ActivityItem;
import com.stockpilot.model.CategoryStock;
import com.stockpilot.model.DailyMovement;
import com.stockpilot.model.DashboardSummary;
import com.stockpilot.model.InventoryHealth;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data access object providing dashboard metrics, category distributions,
 * and movement analytics.
 * Product metrics, category distributions, and health indicators query live SQLite records.
 */
public class DashboardDao {

    private final ProductDao productDao;

    public DashboardDao() {
        this.productDao = new ProductDao();
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
     * Seven-day stock movement history.
     * DEMO DATASET: Inbound vs outbound velocity demonstration trend
     * until the comprehensive transaction recording engine is introduced in Stage 3.
     */
    public List<DailyMovement> fetchStockMovements() {
        return List.of(
                new DailyMovement("Nov 07", 42, 28),
                new DailyMovement("Nov 08", 65, 35),
                new DailyMovement("Nov 09", 30, 52),
                new DailyMovement("Nov 10", 85, 40),
                new DailyMovement("Nov 11", 25, 60),
                new DailyMovement("Nov 12", 70, 45),
                new DailyMovement("Nov 13", 45, 30)
        );
    }

    /**
     * Recent inventory activity events.
     * DEMO DATASET: Initial ledger log entries.
     */
    public List<ActivityItem> fetchRecentActivity() {
        return List.of(
                new ActivityItem("Today, 09:42 AM", "Stock In", "USB-C Docking Station", "+45 units", "Completed"),
                new ActivityItem("Today, 09:15 AM", "Stock Out", "Ergonomic Wireless Mouse", "-18 units", "Completed"),
                new ActivityItem("Yesterday, 05:30 PM", "Stock Alert", "27\" 4K IPS Color Monitor", "6 remaining", "Attention"),
                new ActivityItem("Yesterday, 03:10 PM", "Stock Out", "Mechanical Keyboard (MX Brown)", "-12 units", "Completed"),
                new ActivityItem("Yesterday, 11:05 AM", "Stock Alert", "Cat6 Shielded Patch Cable (5m)", "0 remaining", "Critical")
        );
    }
}
