package com.acme.shop.domain.shipping;

import com.acme.shop.domain.order.Address;
import com.acme.shop.domain.order.Money;
import com.acme.shop.domain.order.OrderId;
import java.time.LocalDateTime;

public class Shipment {

    public enum ShipmentStatus {
        PENDING,
        PICKED_UP,
        IN_TRANSIT,
        DELIVERED,
        RETURNED
    }

    private ShipmentId id;
    private final String trackingNumber;
    private final OrderId orderId;
    private final String carrier;
    private ShipmentStatus status;
    private final Money shippingCost;
    private final Address destination;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;

    public Shipment(ShipmentId id, String trackingNumber, OrderId orderId, String carrier,
                    ShipmentStatus status, Money shippingCost, Address destination,
                    LocalDateTime shippedAt, LocalDateTime deliveredAt) {
        this.id = id;
        this.trackingNumber = trackingNumber;
        this.orderId = orderId;
        this.carrier = carrier;
        this.status = status;
        this.shippingCost = shippingCost;
        this.destination = destination;
        this.shippedAt = shippedAt;
        this.deliveredAt = deliveredAt;
    }

    public static Shipment create(String trackingNumber, OrderId orderId, String carrier,
                                   Money shippingCost, Address destination) {
        return new Shipment(null, trackingNumber, orderId, carrier,
                ShipmentStatus.PENDING, shippingCost, destination, null, null);
    }

    public void ship() {
        if (status != ShipmentStatus.PENDING) {
            throw new IllegalStateException("Shipment must be PENDING to ship");
        }
        this.status = ShipmentStatus.IN_TRANSIT;
        this.shippedAt = LocalDateTime.now();
    }

    public void markDelivered() {
        this.status = ShipmentStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    public ShipmentId getId() { return id; }
    public void setId(ShipmentId id) { this.id = id; }
    public String getTrackingNumber() { return trackingNumber; }
    public OrderId getOrderId() { return orderId; }
    public String getCarrier() { return carrier; }
    public ShipmentStatus getStatus() { return status; }
    public Money getShippingCost() { return shippingCost; }
    public Address getDestination() { return destination; }
    public LocalDateTime getShippedAt() { return shippedAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
}
