package com.acme.shop.domain.shipping;

public record ShipmentId(Long value) {

    public ShipmentId {
        if (value == null) {
            throw new IllegalArgumentException("ShipmentId value must not be null");
        }
    }
}
