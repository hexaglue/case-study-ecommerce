package com.acme.shop.domain.product;

public record ProductId(Long value) {

    public ProductId {
        if (value == null) {
            throw new IllegalArgumentException("ProductId value must not be null");
        }
    }
}
