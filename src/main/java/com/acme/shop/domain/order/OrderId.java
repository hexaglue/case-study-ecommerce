package com.acme.shop.domain.order;

import java.util.UUID;

public record OrderId(UUID value) {

    public OrderId {
        if (value == null) {
            throw new IllegalArgumentException("OrderId value must not be null");
        }
    }

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }
}
