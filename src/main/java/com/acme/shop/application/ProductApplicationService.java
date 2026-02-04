package com.acme.shop.application;

import com.acme.shop.domain.order.Money;
import com.acme.shop.domain.product.Category;
import com.acme.shop.domain.product.Product;
import com.acme.shop.domain.product.ProductId;
import com.acme.shop.ports.in.ProductUseCases;
import com.acme.shop.ports.out.ProductRepository;
import java.util.List;

public class ProductApplicationService implements ProductUseCases {

    private final ProductRepository productRepository;

    public ProductApplicationService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Product createProduct(String name, String description, Money price, String sku, Category category) {
        if (productRepository.findBySku(sku).isPresent()) {
            throw new IllegalArgumentException("SKU already exists: " + sku);
        }
        Product product = Product.create(name, description, price, sku, category);
        return productRepository.save(product);
    }

    @Override
    public Product updatePrice(ProductId productId, Money newPrice) {
        Product product = findProductOrThrow(productId);
        product.updatePrice(newPrice);
        return productRepository.save(product);
    }

    @Override
    public Product deactivate(ProductId productId) {
        Product product = findProductOrThrow(productId);
        product.deactivate();
        return productRepository.save(product);
    }

    @Override
    public Product getProduct(ProductId productId) {
        return findProductOrThrow(productId);
    }

    @Override
    public List<Product> getActiveProducts() {
        return productRepository.findByActiveTrue();
    }

    @Override
    public List<Product> findByCategory(Category category) {
        return productRepository.findByCategory(category);
    }

    @Override
    public List<Product> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    private Product findProductOrThrow(ProductId productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    }
}
