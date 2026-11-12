package com.stockpilot.model;

/**
 * Holds summary metrics displayed on the main StockPilot dashboard.
 */
public class DashboardSummary {

    private final int totalProducts;
    private final int totalStockUnits;
    private final int lowStockItems;
    private final int outOfStockItems;

    public DashboardSummary(int totalProducts, int totalStockUnits, int lowStockItems, int outOfStockItems) {
        this.totalProducts = totalProducts;
        this.totalStockUnits = totalStockUnits;
        this.lowStockItems = lowStockItems;
        this.outOfStockItems = outOfStockItems;
    }

    public int getTotalProducts() {
        return totalProducts;
    }

    public int getTotalStockUnits() {
        return totalStockUnits;
    }

    public int getLowStockItems() {
        return lowStockItems;
    }

    public int getOutOfStockItems() {
        return outOfStockItems;
    }
}

