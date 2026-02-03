package com.acme.shop.domain.shipping;

import java.util.UUID;

public record ShipmentId(UUID value) {

    public ShipmentId {
        if (value == null) {
            throw new IllegalArgumentException("ShipmentId value must not be null");
        }
    }

    public static ShipmentId generate() {
        return new ShipmentId(UUID.randomUUID());
    }
}
