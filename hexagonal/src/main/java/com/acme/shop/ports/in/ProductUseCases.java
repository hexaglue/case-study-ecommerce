package com.acme.shop.ports.in;

import com.acme.shop.domain.order.Money;
import com.acme.shop.domain.product.Category;
import com.acme.shop.domain.product.Product;
import com.acme.shop.domain.product.ProductId;
import java.util.List;

public interface ProductUseCases {
    Product createProduct(String name, String description, Money price, String sku, Category category);
    Product updatePrice(ProductId productId, Money newPrice);
    Product deactivate(ProductId productId);
    Product getProduct(ProductId productId);
    List<Product> getActiveProducts();
    List<Product> findByCategory(Category category);
    List<Product> searchByName(String name);
}
