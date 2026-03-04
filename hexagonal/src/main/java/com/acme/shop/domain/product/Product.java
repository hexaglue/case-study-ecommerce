package com.acme.shop.domain.product;

import com.acme.shop.domain.order.Money;

public class Product {

    private ProductId id;
    private final String name;
    private String description;
    private Money price;
    private final String sku;
    private final Category category;
    private boolean active;

    public Product(ProductId id, String name, String description, Money price,
                   String sku, Category category, boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.sku = sku;
        this.category = category;
        this.active = active;
    }

    public static Product create(String name, String description, Money price, String sku, Category category) {
        return new Product(null, name, description, price, sku, category, true);
    }

    public void updatePrice(Money newPrice) {
        if (newPrice.amount().signum() <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        this.price = newPrice;
    }

    public void deactivate() {
        this.active = false;
    }

    public ProductId getId() { return id; }
    public void setId(ProductId id) { this.id = id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Money getPrice() { return price; }
    public String getSku() { return sku; }
    public Category getCategory() { return category; }
    public boolean isActive() { return active; }
}
