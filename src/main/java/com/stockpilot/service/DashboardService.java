package com.stockpilot.service;

import com.stockpilot.dao.DashboardDao;
import com.stockpilot.model.ActivityItem;
import com.stockpilot.model.DashboardSummary;

import java.util.List;

/**
 * Service layer coordinating dashboard data retrieval.
 */
public class DashboardService {

    private final DashboardDao dashboardDao;

    public DashboardService() {
        this.dashboardDao = new DashboardDao();
    }

    public DashboardSummary getDashboardSummary() {
        return dashboardDao.fetchSummary();
    }

    public List<ActivityItem> getRecentActivity() {
        return dashboardDao.fetchRecentActivity();
    }
}

