package com.stockpilot.dao;

import com.stockpilot.model.ActivityItem;
import com.stockpilot.model.CategoryStock;
import com.stockpilot.model.DailyMovement;
import com.stockpilot.model.DashboardSummary;
import com.stockpilot.model.InventoryHealth;

import java.util.List;

/**
 * Data access object providing inventory metrics, activity logs, and analytics.
 * NOTE: Currently populated with structured DEMO DATA for dashboard evaluation
 * until SQLite persistence is integrated in the next milestone.
 */
public class DashboardDao {

    /**
     * Summary card metrics.
     * DEMO DATA: 148 products across catalog, 3,420 total units in storage.
     */
    public DashboardSummary fetchSummary() {
        return new DashboardSummary(148, 3420, 12, 4);
    }

    /**
     * Product counts and unit quantities grouped by major inventory category.
     * DEMO DATA: 5 industrial catalog categories summing to 148 products and 3,420 units.
     */
    public List<CategoryStock> fetchCategoryStocks() {
        return List.of(
                new CategoryStock("Peripherals", 48, 1120),
                new CategoryStock("Networking", 32, 780),
                new CategoryStock("Displays", 18, 340),
                new CategoryStock("Storage", 26, 690),
                new CategoryStock("Cables & Power", 24, 490)
        );
    }

    /**
     * Seven-day stock movement history (Stock In vs Stock Out).
     * DEMO DATA: Last 7 days of warehouse movement volume.
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
     * Overall inventory health indicators.
     * DEMO DATA: 132 healthy, 12 low stock buffer warnings, 4 depleted stock alerts.
     */
    public InventoryHealth fetchInventoryHealth() {
        return new InventoryHealth(132, 12, 4);
    }

    /**
     * Recent inventory activity events.
     * DEMO DATA: 5 recent events for testing compact table display.
     */
    public List<ActivityItem> fetchRecentActivity() {
        return List.of(
                new ActivityItem("Today, 09:42 AM", "Stock In", "USB-C Docking Station", "+45 units", "Completed"),
                new ActivityItem("Today, 09:15 AM", "Stock Out", "Ergonomic Wireless Mouse", "-18 units", "Completed"),
                new ActivityItem("Yesterday, 05:30 PM", "Stock Alert", "27\" 4K IPS Monitor", "6 remaining", "Attention"),
                new ActivityItem("Yesterday, 03:10 PM", "Stock Out", "Mechanical Keyboard (MX Brown)", "-12 units", "Completed"),
                new ActivityItem("Yesterday, 11:05 AM", "Stock Alert", "Cat6 Shielded Patch Cable (5m)", "0 remaining", "Critical")
        );
    }
}
