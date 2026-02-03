package com.acme.shop.domain.inventory;

import java.time.LocalDateTime;
import java.util.UUID;

public class StockMovement {

    public enum MovementType {
        RECEIVED,
        RESERVED,
        RELEASED,
        SHIPPED,
        ADJUSTED
    }

    private UUID id;
    private final MovementType type;
    private final int quantity;
    private final String reason;
    private final LocalDateTime occurredAt;

    public StockMovement(UUID id, MovementType type, int quantity, String reason, LocalDateTime occurredAt) {
        this.id = id;
        this.type = type;
        this.quantity = quantity;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    public static StockMovement create(MovementType type, int quantity, String reason) {
        return new StockMovement(UUID.randomUUID(), type, quantity, reason, LocalDateTime.now());
    }

    public static StockMovement reconstitute(UUID id, MovementType type, int quantity,
                                              String reason, LocalDateTime occurredAt) {
        return new StockMovement(id, type, quantity, reason, occurredAt);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public MovementType getType() { return type; }
    public int getQuantity() { return quantity; }
    public String getReason() { return reason; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
