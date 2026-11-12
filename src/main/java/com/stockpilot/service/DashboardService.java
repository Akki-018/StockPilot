package com.stockpilot.service;

import com.stockpilot.dao.DashboardDao;
import com.stockpilot.model.ActivityItem;
import com.stockpilot.model.CategoryStock;
import com.stockpilot.model.DailyMovement;
import com.stockpilot.model.DashboardSummary;
import com.stockpilot.model.InventoryHealth;

import java.util.List;

/**
 * Service layer coordinating dashboard analytics, stock metrics, and activity.
 */
public class DashboardService {

    private final DashboardDao dashboardDao;

    public DashboardService() {
        this.dashboardDao = new DashboardDao();
    }

    public DashboardSummary getDashboardSummary() {
        return dashboardDao.fetchSummary();
    }

    public List<CategoryStock> getCategoryStocks() {
        return dashboardDao.fetchCategoryStocks();
    }

    public List<DailyMovement> getStockMovements() {
        return dashboardDao.fetchStockMovements();
    }

    public InventoryHealth getInventoryHealth() {
        return dashboardDao.fetchInventoryHealth();
    }

    public List<ActivityItem> getRecentActivity() {
        return dashboardDao.fetchRecentActivity();
    }
}
