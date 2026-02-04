package com.acme.shop.ports.in;

import com.acme.shop.domain.inventory.Inventory;
import com.acme.shop.domain.product.ProductId;

public interface InventoryUseCases {
    Inventory initializeStock(ProductId productId, int initialQuantity);
    int getAvailableQuantity(ProductId productId);
}
