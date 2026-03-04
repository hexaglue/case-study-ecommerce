package com.acme.shop.domain.customer;

public record Email(String value) {

    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email must not be null or blank");
        }
        if (!value.contains("@")) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }
}
