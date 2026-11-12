package com.stockpilot.service;

import com.stockpilot.dao.ProductDao;
import com.stockpilot.dao.TransactionDao;
import com.stockpilot.model.CategoryStock;
import com.stockpilot.model.Product;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service layer coordinating product business logic, input validation,
 * duplicate SKU prevention, and inventory calculations.
 */
public class ProductService {

    private final ProductDao productDao;
    private final TransactionDao transactionDao;

    public ProductService() {
        this.productDao = new ProductDao();
        this.transactionDao = new TransactionDao();
    }

    public ProductService(ProductDao productDao, TransactionDao transactionDao) {
        this.productDao = productDao;
        this.transactionDao = transactionDao;
    }

    public List<Product> getAllProducts() {
        return productDao.findAll();
    }

    public Optional<Product> getProductById(int id) {
        return productDao.findById(id);
    }

    public List<Product> searchAndFilter(String query, String category, String status) {
        return productDao.searchAndFilter(query, category, status);
    }

    public List<String> getAllCategories() {
        return productDao.findAllCategories();
    }

    /**
     * Validates and persists a product (either insert or update).
     *
     * @param product the product entity
     * @return the saved product
     * @throws IllegalArgumentException if validation rules fail
     * @throws SQLException if a database error occurs
     */
    public Product saveProduct(Product product) throws SQLException {
        validateProduct(product);

        if (product.getId() == null) {
            return productDao.insert(product);
        } else {
            boolean updated = productDao.update(product);
            if (!updated) {
                throw new SQLException("Failed to update product with ID: " + product.getId());
            }
            return product;
        }
    }

    /**
     * Deletes a product by ID.
     * Enforces audit preservation: prevents deletion if historical transactions exist.
     */
    public boolean deleteProduct(int id) throws SQLException {
        if (transactionDao.hasTransactionsForProduct(id)) {
            throw new IllegalStateException("Cannot delete product: Historical transactions exist for this product. Inventory audit ledger must be preserved.");
        }
        return productDao.delete(id);
    }

    /**
     * Validates all input constraints for a product.
     */
    public void validateProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null.");
        }

        // 1. SKU validation
        String sku = product.getSku();
        if (sku == null || sku.trim().isEmpty()) {
            throw new IllegalArgumentException("SKU is required.");
        }
        sku = sku.trim();
        if (sku.length() < 3 || sku.length() > 30) {
            throw new IllegalArgumentException("SKU must be between 3 and 30 characters.");
        }
        if (productDao.existsBySku(sku, product.getId())) {
            throw new IllegalArgumentException("A product with SKU '" + sku + "' already exists.");
        }

        // 2. Name validation
        String name = product.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required.");
        }
        if (name.trim().length() < 2 || name.trim().length() > 100) {
            throw new IllegalArgumentException("Product name must be between 2 and 100 characters.");
        }

        // 3. Category validation
        String category = product.getCategory();
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category is required.");
        }

        // 4. Quantity validation
        if (product.getQuantity() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }

        // 5. Minimum stock validation
        if (product.getMinimumStock() < 0) {
            throw new IllegalArgumentException("Minimum stock threshold cannot be negative.");
        }

        // 6. Unit price validation
        BigDecimal price = product.getUnitPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be null or negative.");
        }
    }

    // ==================== Live Aggregate Queries for Dashboard ====================

    public int getTotalProductsCount() {
        return productDao.countTotalProducts();
    }

    public int getTotalStockUnits() {
        return productDao.sumTotalUnits();
    }

    public int getLowStockProductsCount() {
        return productDao.countLowStockProducts();
    }

    public int getOutOfStockProductsCount() {
        return productDao.countOutOfStockProducts();
    }

    public BigDecimal getInventoryValuation() {
        return productDao.sumInventoryValuation();
    }

    public List<CategoryStock> getCategoryStockSummary() {
        return productDao.fetchCategoryStockSummary();
    }
}

