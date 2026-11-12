package com.stockpilot.dao;

import com.stockpilot.model.ActivityItem;
import com.stockpilot.model.DashboardSummary;

import java.util.List;

/**
 * Data access object for dashboard metrics and recent activity.
 * Currently supplies initial sample records until SQLite DAOs are connected.
 */
public class DashboardDao {

    public DashboardSummary fetchSummary() {
        return new DashboardSummary(148, 3420, 12, 4);
    }

    public List<ActivityItem> fetchRecentActivity() {
        return List.of(
                new ActivityItem("Today, 09:42 AM", "Stock In", "USB-C Docking Station", "+45 units", "Completed"),
                new ActivityItem("Today, 09:15 AM", "Stock Out", "Ergonomic Wireless Mouse", "-18 units", "Completed"),
                new ActivityItem("Yesterday, 05:30 PM", "Low Stock Alert", "27\" 4K IPS Monitor", "6 remaining", "Attention"),
                new ActivityItem("Yesterday, 03:10 PM", "Stock Out", "Mechanical Keyboard (MX Brown)", "-12 units", "Completed"),
                new ActivityItem("Yesterday, 11:05 AM", "Out of Stock", "Cat6 Shielded Patch Cable (5m)", "0 remaining", "Critical")
        );
    }
}

