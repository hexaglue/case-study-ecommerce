package com.acme.shop.ports.out;

import com.acme.shop.domain.product.Category;
import com.acme.shop.domain.product.Product;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(Long id);
    Optional<Product> findBySku(String sku);
    List<Product> findByCategory(Category category);
    List<Product> findByActiveTrue();
    List<Product> findByNameContainingIgnoreCase(String name);
}
