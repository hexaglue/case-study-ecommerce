package com.acme.shop.domain.inventory;

import java.time.LocalDateTime;

public class StockMovement {

    public enum MovementType {
        RECEIVED,
        RESERVED,
        RELEASED,
        SHIPPED,
        ADJUSTED
    }

    private Long id;
    private final MovementType type;
    private final int quantity;
    private final String reason;
    private final LocalDateTime occurredAt;

    public StockMovement(Long id, MovementType type, int quantity, String reason, LocalDateTime occurredAt) {
        this.id = id;
        this.type = type;
        this.quantity = quantity;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    public static StockMovement create(MovementType type, int quantity, String reason) {
        return new StockMovement(null, type, quantity, reason, LocalDateTime.now());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MovementType getType() { return type; }
    public int getQuantity() { return quantity; }
    public String getReason() { return reason; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
