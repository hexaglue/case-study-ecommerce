package com.acme.shop.domain.payment;

import java.util.UUID;

public record PaymentId(UUID value) {

    public PaymentId {
        if (value == null) {
            throw new IllegalArgumentException("PaymentId value must not be null");
        }
    }

    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID());
    }
}
