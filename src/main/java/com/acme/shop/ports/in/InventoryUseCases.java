package com.acme.shop.ports.in;

import com.acme.shop.domain.inventory.Inventory;

public interface InventoryUseCases {
    Inventory initializeStock(Long productId, int initialQuantity);
    void reserveStock(Long productId, int quantity);
    void releaseStock(Long productId, int quantity);
    void shipStock(Long productId, int quantity);
    int getAvailableQuantity(Long productId);
}
