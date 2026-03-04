package com.acme.shop.service;

import com.acme.shop.model.Category;
import com.acme.shop.model.Product;
import com.acme.shop.repository.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anti-pattern: domain service marked with @Service,
 * contains purely domain logic (catalog browsing).
 */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final ProductRepository productRepository;

    public CatalogService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Map<Category, List<Product>> getProductsByCategory() {
        return productRepository.findByActiveTrue().stream()
                .collect(Collectors.groupingBy(Product::getCategory));
    }

    public List<Product> searchProducts(String query) {
        if (query == null || query.isBlank()) {
            return productRepository.findByActiveTrue();
        }
        return productRepository.findByNameContainingIgnoreCase(query);
    }
}
