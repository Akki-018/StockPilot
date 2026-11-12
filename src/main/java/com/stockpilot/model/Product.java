package com.stockpilot.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Domain entity representing an inventory product SKU record.
 */
public class Product {

    private Integer id;
    private String sku;
    private String name;
    private String description;
    private String category;
    private int quantity;
    private int minimumStock;
    private BigDecimal unitPrice;
    private Integer supplierId;
    private String createdAt;
    private String updatedAt;

    public Product() {
        this.quantity = 0;
        this.minimumStock = 0;
        this.unitPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public Product(Integer id, String sku, String name, String description, String category,
                   int quantity, int minimumStock, BigDecimal unitPrice, Integer supplierId,
                   String createdAt, String updatedAt) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.category = category;
        this.quantity = quantity;
        this.minimumStock = minimumStock;
        this.unitPrice = unitPrice != null ? unitPrice.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.supplierId = supplierId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Product(String sku, String name, String description, String category,
                   int quantity, int minimumStock, BigDecimal unitPrice) {
        this(null, sku, name, description, category, quantity, minimumStock, unitPrice, null, null, null);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getMinimumStock() {
        return minimumStock;
    }

    public void setMinimumStock(int minimumStock) {
        this.minimumStock = minimumStock;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice != null ? unitPrice.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public Integer getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Integer supplierId) {
        this.supplierId = supplierId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Determines the operational stock health status:
     * - "Out of Stock" if quantity is 0 or less
     * - "Low Stock" if quantity is at or below minimum reorder threshold
     * - "Healthy" otherwise
     */
    public String getStockStatus() {
        if (quantity <= 0) {
            return "Out of Stock";
        }
        if (quantity <= minimumStock) {
            return "Low Stock";
        }
        return "Healthy";
    }

    /**
     * Calculates the total inventory value of this product on hand (quantity * unit price).
     */
    public BigDecimal getTotalValuation() {
        if (unitPrice == null || quantity <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", sku='" + sku + '\'' +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", quantity=" + quantity +
                ", minimumStock=" + minimumStock +
                ", unitPrice=" + unitPrice +
                '}';
    }
}
