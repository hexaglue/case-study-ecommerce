package com.acme.shop.domain.payment;

public record PaymentId(Long value) {

    public PaymentId {
        if (value == null) {
            throw new IllegalArgumentException("PaymentId value must not be null");
        }
    }
}
