package com.acme.shop.domain.order;

import com.acme.shop.domain.product.ProductId;

public class OrderLine {

    private Long id;
    private final ProductId productId;
    private final String productName;
    private final Quantity quantity;
    private final Money unitPrice;
    private final Money lineTotal;

    public OrderLine(Long id, ProductId productId, String productName, Quantity quantity, Money unitPrice) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = unitPrice.multiply(quantity.value());
    }

    public static OrderLine create(ProductId productId, String productName, Quantity quantity, Money unitPrice) {
        return new OrderLine(null, productId, productName, quantity, unitPrice);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ProductId getProductId() { return productId; }
    public String getProductName() { return productName; }
    public Quantity getQuantity() { return quantity; }
    public Money getUnitPrice() { return unitPrice; }
    public Money getLineTotal() { return lineTotal; }
}
