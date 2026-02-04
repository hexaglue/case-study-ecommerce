package com.acme.shop.application;

import com.acme.shop.domain.inventory.Inventory;
import com.acme.shop.domain.product.ProductId;
import com.acme.shop.ports.in.InventoryUseCases;
import com.acme.shop.ports.out.InventoryRepository;
import com.acme.shop.ports.out.ProductRepository;

public class InventoryApplicationService implements InventoryUseCases {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public InventoryApplicationService(InventoryRepository inventoryRepository, ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Inventory initializeStock(ProductId productId, int initialQuantity) {
        productRepository
                .findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (inventoryRepository.findByProductId(productId).isPresent()) {
            throw new IllegalArgumentException("Inventory already exists for product: " + productId);
        }

        Inventory inventory = Inventory.initialize(productId, initialQuantity);
        return inventoryRepository.save(inventory);
    }

    @Override
    public void reserveStock(ProductId productId, int quantity) {
        Inventory inventory = findInventoryOrThrow(productId);
        inventory.reserve(quantity);
        inventoryRepository.save(inventory);
    }

    @Override
    public void releaseStock(ProductId productId, int quantity) {
        Inventory inventory = findInventoryOrThrow(productId);
        inventory.release(quantity);
        inventoryRepository.save(inventory);
    }

    @Override
    public void shipStock(ProductId productId, int quantity) {
        Inventory inventory = findInventoryOrThrow(productId);
        inventory.ship(quantity);
        inventoryRepository.save(inventory);
    }

    @Override
    public int getAvailableQuantity(ProductId productId) {
        return findInventoryOrThrow(productId).getAvailableQuantity();
    }

    private Inventory findInventoryOrThrow(ProductId productId) {
        return inventoryRepository
                .findByProductId(productId)
                .orElseThrow(() -> new IllegalStateException("No inventory record for product: " + productId));
    }
}
