package com.stockpilot.model;

/**
 * Encapsulates overall catalog health: healthy, low stock, and depleted items.
 */
public class InventoryHealth {

    private final int healthyItems;
    private final int lowStockItems;
    private final int outOfStockItems;

    public InventoryHealth(int healthyItems, int lowStockItems, int outOfStockItems) {
        this.healthyItems = healthyItems;
        this.lowStockItems = lowStockItems;
        this.outOfStockItems = outOfStockItems;
    }

    public int getHealthyItems() {
        return healthyItems;
    }

    public int getLowStockItems() {
        return lowStockItems;
    }

    public int getOutOfStockItems() {
        return outOfStockItems;
    }

    public int getTotalItems() {
        return healthyItems + lowStockItems + outOfStockItems;
    }

    public double getHealthyPercentage() {
        int total = getTotalItems();
        return total == 0 ? 0.0 : (healthyItems * 100.0) / total;
    }

    public double getLowStockPercentage() {
        int total = getTotalItems();
        return total == 0 ? 0.0 : (lowStockItems * 100.0) / total;
    }

    public double getOutOfStockPercentage() {
        int total = getTotalItems();
        return total == 0 ? 0.0 : (outOfStockItems * 100.0) / total;
    }
}
