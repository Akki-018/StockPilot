package com.stockpilot.model;

/**
 * Domain entity representing an audited inventory stock movement transaction.
 */
public class Transaction {

    private Integer id;
    private int productId;
    private String productSku;
    private String productName;
    private String transactionType; // "STOCK_IN" or "STOCK_OUT"
    private int quantity;
    private int previousStock;
    private int resultingStock;
    private String transactionDate;
    private String notes;

    public Transaction() {
    }

    public Transaction(Integer id, int productId, String productSku, String productName,
                       String transactionType, int quantity, int previousStock, int resultingStock,
                       String transactionDate, String notes) {
        this.id = id;
        this.productId = productId;
        this.productSku = productSku;
        this.productName = productName;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.previousStock = previousStock;
        this.resultingStock = resultingStock;
        this.transactionDate = transactionDate;
        this.notes = notes;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductSku() {
        return productSku;
    }

    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getPreviousStock() {
        return previousStock;
    }

    public void setPreviousStock(int previousStock) {
        this.previousStock = previousStock;
    }

    public int getResultingStock() {
        return resultingStock;
    }

    public void setResultingStock(int resultingStock) {
        this.resultingStock = resultingStock;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isStockIn() {
        return "STOCK_IN".equalsIgnoreCase(transactionType);
    }

    public String getFormattedType() {
        return isStockIn() ? "STOCK IN" : "STOCK OUT";
    }

    public String getFormattedDelta() {
        return (isStockIn() ? "+" : "-") + quantity + " units";
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", productSku='" + productSku + '\'' +
                ", transactionType='" + transactionType + '\'' +
                ", quantity=" + quantity +
                ", previousStock=" + previousStock +
                ", resultingStock=" + resultingStock +
                ", transactionDate='" + transactionDate + '\'' +
                '}';
    }
}

