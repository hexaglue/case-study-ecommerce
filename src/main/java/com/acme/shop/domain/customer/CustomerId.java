package com.acme.shop.domain.customer;

public record CustomerId(Long value) {

    public CustomerId {
        if (value == null) {
            throw new IllegalArgumentException("CustomerId value must not be null");
        }
    }
}
