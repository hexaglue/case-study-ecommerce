package com.acme.shop.ports.out;

import com.acme.shop.domain.inventory.Inventory;
import com.acme.shop.domain.inventory.InventoryId;
import com.acme.shop.domain.product.ProductId;
import java.util.Optional;

public interface InventoryRepository {
    Inventory save(Inventory inventory);
    Optional<Inventory> findById(InventoryId id);
    Optional<Inventory> findByProductId(ProductId productId);
}
