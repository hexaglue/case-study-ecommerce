package com.acme.shop.domain.order;

public record Quantity(int value) {

    public Quantity {
        if (value < 0) {
            throw new IllegalArgumentException("Quantity must be >= 0, got: " + value);
        }
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }
}
