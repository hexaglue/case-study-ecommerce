package com.acme.shop.domain.inventory;

import com.acme.shop.domain.product.ProductId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Inventory {

    private InventoryId id;
    private final ProductId productId;
    private int quantityOnHand;
    private int reservedQuantity;
    private int reorderThreshold;
    private final List<StockMovement> movements = new ArrayList<>();

    public Inventory(InventoryId id, ProductId productId, int quantityOnHand,
                     int reservedQuantity, int reorderThreshold, List<StockMovement> movements) {
        this.id = id;
        this.productId = productId;
        this.quantityOnHand = quantityOnHand;
        this.reservedQuantity = reservedQuantity;
        this.reorderThreshold = reorderThreshold;
        if (movements != null) {
            this.movements.addAll(movements);
        }
    }

    public static Inventory initialize(ProductId productId, int initialQuantity) {
        Inventory inventory = new Inventory(null, productId, initialQuantity, 0, 10, null);
        inventory.addMovement(StockMovement.MovementType.RECEIVED, initialQuantity, "Initial stock");
        return inventory;
    }

    public void reserve(int qty) {
        if (getAvailableQuantity() < qty) {
            throw new IllegalStateException(
                    "Insufficient stock for product " + productId + ": available=" + getAvailableQuantity()
                            + ", requested=" + qty);
        }
        this.reservedQuantity += qty;
        addMovement(StockMovement.MovementType.RESERVED, qty, "Order reservation");
    }

    public void release(int qty) {
        this.reservedQuantity = Math.max(0, this.reservedQuantity - qty);
        addMovement(StockMovement.MovementType.RELEASED, qty, "Order cancellation");
    }

    public void ship(int qty) {
        this.quantityOnHand -= qty;
        this.reservedQuantity = Math.max(0, this.reservedQuantity - qty);
        addMovement(StockMovement.MovementType.SHIPPED, qty, "Order shipped");
    }

    public int getAvailableQuantity() {
        return quantityOnHand - reservedQuantity;
    }

    private void addMovement(StockMovement.MovementType type, int quantity, String reason) {
        movements.add(StockMovement.create(type, quantity, reason));
    }

    public InventoryId getId() { return id; }
    public void setId(InventoryId id) { this.id = id; }
    public ProductId getProductId() { return productId; }
    public int getQuantityOnHand() { return quantityOnHand; }
    public int getReservedQuantity() { return reservedQuantity; }
    public int getReorderThreshold() { return reorderThreshold; }
    public List<StockMovement> getMovements() { return Collections.unmodifiableList(movements); }
}
