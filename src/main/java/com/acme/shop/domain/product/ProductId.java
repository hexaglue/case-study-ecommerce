package com.acme.shop.domain.product;

import java.util.UUID;

public record ProductId(UUID value) {

    public ProductId {
        if (value == null) {
            throw new IllegalArgumentException("ProductId value must not be null");
        }
    }

    public static ProductId generate() {
        return new ProductId(UUID.randomUUID());
    }
}
