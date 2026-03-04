package com.acme.shop.ports.in;

import com.acme.shop.domain.inventory.Inventory;
import com.acme.shop.domain.product.ProductId;

public interface InventoryUseCases {
    Inventory initializeStock(ProductId productId, int initialQuantity);
    void reserveStock(ProductId productId, int quantity);
    void releaseStock(ProductId productId, int quantity);
    void shipStock(ProductId productId, int quantity);
    int getAvailableQuantity(ProductId productId);
}
