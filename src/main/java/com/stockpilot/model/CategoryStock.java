package com.stockpilot.model;

/**
 * Represents stock unit volume and SKU counts for an inventory category.
 */
public class CategoryStock {

    private final String categoryName;
    private final int productCount;
    private final int unitCount;

    public CategoryStock(String categoryName, int productCount, int unitCount) {
        this.categoryName = categoryName;
        this.productCount = productCount;
        this.unitCount = unitCount;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public int getProductCount() {
        return productCount;
    }

    public int getUnitCount() {
        return unitCount;
    }
}

