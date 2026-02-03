package com.acme.shop.domain.order;

public record OrderId(Long value) {

    public OrderId {
        if (value == null) {
            throw new IllegalArgumentException("OrderId value must not be null");
        }
    }
}
