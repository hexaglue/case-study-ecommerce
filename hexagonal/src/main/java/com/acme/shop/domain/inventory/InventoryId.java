package com.acme.shop.domain.inventory;

public record InventoryId(Long value) {

    public InventoryId {
        if (value == null) {
            throw new IllegalArgumentException("InventoryId value must not be null");
        }
    }
}
