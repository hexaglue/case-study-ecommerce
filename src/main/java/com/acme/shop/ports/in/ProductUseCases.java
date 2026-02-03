package com.acme.shop.ports.in;

import com.acme.shop.domain.product.Category;
import com.acme.shop.domain.product.Product;
import java.math.BigDecimal;
import java.util.List;

public interface ProductUseCases {
    Product createProduct(String name, String description, BigDecimal price, String currency, String sku, Category category);
    Product updatePrice(Long productId, BigDecimal newPrice);
    Product deactivate(Long productId);
    Product getProduct(Long productId);
    List<Product> getActiveProducts();
    List<Product> findByCategory(Category category);
    List<Product> searchByName(String name);
}
