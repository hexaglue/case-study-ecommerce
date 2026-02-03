package com.acme.shop.domain.inventory;

import java.util.UUID;

public record InventoryId(UUID value) {

    public InventoryId {
        if (value == null) {
            throw new IllegalArgumentException("InventoryId value must not be null");
        }
    }

    public static InventoryId generate() {
        return new InventoryId(UUID.randomUUID());
    }
}
