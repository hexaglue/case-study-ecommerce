package com.acme.shop.service;

import com.acme.shop.model.Category;
import com.acme.shop.model.Product;
import com.acme.shop.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product createProduct(String name, String description, BigDecimal price, String currency, String sku,
            Category category) {
        if (productRepository.findBySku(sku).isPresent()) {
            throw new IllegalArgumentException("SKU already exists: " + sku);
        }
        Product product = new Product(name, price, currency, sku, category);
        product.setDescription(description);
        return productRepository.save(product);
    }

    public Product updatePrice(Long productId, BigDecimal newPrice) {
        Product product = productRepository
                .findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        // Anti-pattern: no price validation in domain
        if (newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        product.setPrice(newPrice);
        return productRepository.save(product);
    }

    public Product deactivate(Long productId) {
        Product product = productRepository
                .findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        product.setActive(false);
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Product> findByCategory(Category category) {
        return productRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public List<Product> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public List<Product> getActiveProducts() {
        return productRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Product getProduct(Long productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    }
}
