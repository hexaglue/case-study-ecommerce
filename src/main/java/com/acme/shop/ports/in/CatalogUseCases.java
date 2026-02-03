package com.acme.shop.ports.in;

import com.acme.shop.domain.product.Category;
import com.acme.shop.domain.product.Product;
import java.util.List;
import java.util.Map;

public interface CatalogUseCases {
    Map<Category, List<Product>> getProductsByCategory();
    List<Product> searchProducts(String query);
}
