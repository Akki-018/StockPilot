package com.stockpilot.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Holds summary metrics displayed on the main StockPilot dashboard,
 * calculated directly from real SQLite database queries.
 */
public class DashboardSummary {

    private final int totalProducts;
    private final int totalStockUnits;
    private final int lowStockItems;
    private final int outOfStockItems;
    private final BigDecimal inventoryValuation;

    public DashboardSummary(int totalProducts, int totalStockUnits, int lowStockItems,
                            int outOfStockItems, BigDecimal inventoryValuation) {
        this.totalProducts = totalProducts;
        this.totalStockUnits = totalStockUnits;
        this.lowStockItems = lowStockItems;
        this.outOfStockItems = outOfStockItems;
        this.inventoryValuation = inventoryValuation != null
                ? inventoryValuation.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public DashboardSummary(int totalProducts, int totalStockUnits, int lowStockItems, int outOfStockItems) {
        this(totalProducts, totalStockUnits, lowStockItems, outOfStockItems, BigDecimal.ZERO);
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

    public BigDecimal getInventoryValuation() {
        return inventoryValuation;
    }
}
