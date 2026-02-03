package com.acme.shop.application;

import com.acme.shop.domain.inventory.Inventory;
import com.acme.shop.domain.inventory.StockMovement;
import com.acme.shop.domain.product.Product;
import com.acme.shop.ports.in.InventoryUseCases;
import com.acme.shop.ports.out.InventoryRepository;
import com.acme.shop.ports.out.ProductRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryApplicationService implements InventoryUseCases {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public InventoryApplicationService(InventoryRepository inventoryRepository, ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Inventory initializeStock(Long productId, int initialQuantity) {
        Product product = productRepository
                .findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (inventoryRepository.findByProductId(productId).isPresent()) {
            throw new IllegalArgumentException("Inventory already exists for product: " + productId);
        }

        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setQuantityOnHand(initialQuantity);
        inventory.setReservedQuantity(0);
        inventory.setReorderThreshold(10);

        Inventory saved = inventoryRepository.save(inventory);
        addMovement(saved, StockMovement.MovementType.RECEIVED, initialQuantity, "Initial stock");
        return saved;
    }

    @Override
    public void reserveStock(Long productId, int quantity) {
        Inventory inventory = findInventoryOrThrow(productId);

        if (inventory.getAvailableQuantity() < quantity) {
            throw new IllegalStateException(
                    "Insufficient stock for product " + productId + ": available=" + inventory.getAvailableQuantity()
                            + ", requested=" + quantity);
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventoryRepository.save(inventory);
        addMovement(inventory, StockMovement.MovementType.RESERVED, quantity, "Order reservation");
    }

    @Override
    public void releaseStock(Long productId, int quantity) {
        Inventory inventory = findInventoryOrThrow(productId);
        inventory.setReservedQuantity(Math.max(0, inventory.getReservedQuantity() - quantity));
        inventoryRepository.save(inventory);
        addMovement(inventory, StockMovement.MovementType.RELEASED, quantity, "Order cancellation");
    }

    @Override
    public void shipStock(Long productId, int quantity) {
        Inventory inventory = findInventoryOrThrow(productId);
        inventory.setQuantityOnHand(inventory.getQuantityOnHand() - quantity);
        inventory.setReservedQuantity(Math.max(0, inventory.getReservedQuantity() - quantity));
        inventoryRepository.save(inventory);
        addMovement(inventory, StockMovement.MovementType.SHIPPED, quantity, "Order shipped");
    }

    @Override
    @Transactional(readOnly = true)
    public int getAvailableQuantity(Long productId) {
        return findInventoryOrThrow(productId).getAvailableQuantity();
    }

    private Inventory findInventoryOrThrow(Long productId) {
        return inventoryRepository
                .findByProductId(productId)
                .orElseThrow(() -> new IllegalStateException("No inventory record for product: " + productId));
    }

    private void addMovement(Inventory inventory, StockMovement.MovementType type, int quantity, String reason) {
        StockMovement movement = new StockMovement();
        movement.setInventory(inventory);
        movement.setType(type);
        movement.setQuantity(quantity);
        movement.setReason(reason);
        movement.setOccurredAt(LocalDateTime.now());
        inventory.getMovements().add(movement);
    }
}
