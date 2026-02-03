package com.acme.shop.ports.out;

import com.acme.shop.domain.inventory.Inventory;
import java.util.Optional;

public interface InventoryRepository {
    Inventory save(Inventory inventory);
    Optional<Inventory> findById(Long id);
    Optional<Inventory> findByProductId(Long productId);
}
