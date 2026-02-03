package com.acme.shop.domain.order;

import com.acme.shop.domain.product.ProductId;
import java.util.UUID;

public class OrderLine {

    private UUID id;
    private final ProductId productId;
    private final String productName;
    private final Quantity quantity;
    private final Money unitPrice;
    private final Money lineTotal;

    public OrderLine(UUID id, ProductId productId, String productName, Quantity quantity, Money unitPrice) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = unitPrice.multiply(quantity.value());
    }

    public static OrderLine create(ProductId productId, String productName, Quantity quantity, Money unitPrice) {
        return new OrderLine(UUID.randomUUID(), productId, productName, quantity, unitPrice);
    }

    public static OrderLine reconstitute(UUID id, ProductId productId, String productName,
                                          Quantity quantity, Money unitPrice) {
        return new OrderLine(id, productId, productName, quantity, unitPrice);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public ProductId getProductId() { return productId; }
    public String getProductName() { return productName; }
    public Quantity getQuantity() { return quantity; }
    public Money getUnitPrice() { return unitPrice; }
    public Money getLineTotal() { return lineTotal; }
}
