package com.acme.shop.domain.customer;

import java.util.UUID;

public record CustomerId(UUID value) {

    public CustomerId {
        if (value == null) {
            throw new IllegalArgumentException("CustomerId value must not be null");
        }
    }

    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID());
    }
}
