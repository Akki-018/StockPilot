package com.stockpilot.model;

/**
 * Represents a single inventory event row in the Recent Activity table.
 */
public class ActivityItem {

    private final String timestamp;
    private final String action;
    private final String itemName;
    private final String quantityChange;
    private final String status;

    public ActivityItem(String timestamp, String action, String itemName, String quantityChange, String status) {
        this.timestamp = timestamp;
        this.action = action;
        this.itemName = itemName;
        this.quantityChange = quantityChange;
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getAction() {
        return action;
    }

    public String getItemName() {
        return itemName;
    }

    public String getQuantityChange() {
        return quantityChange;
    }

    public String getStatus() {
        return status;
    }
}

