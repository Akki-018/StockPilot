package com.stockpilot.model;

/**
 * Represents daily incoming and outgoing inventory units for trend analysis.
 */
public class DailyMovement {

    private final String dateLabel;
    private final int stockInUnits;
    private final int stockOutUnits;

    public DailyMovement(String dateLabel, int stockInUnits, int stockOutUnits) {
        this.dateLabel = dateLabel;
        this.stockInUnits = stockInUnits;
        this.stockOutUnits = stockOutUnits;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public int getStockInUnits() {
        return stockInUnits;
    }

    public int getStockOutUnits() {
        return stockOutUnits;
    }
}
