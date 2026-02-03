package com.acme.shop.domain.order;

import com.acme.shop.domain.customer.CustomerId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Order {

    private OrderId id;
    private final String orderNumber;
    private final CustomerId customerId;
    private final List<OrderLine> lines = new ArrayList<>();
    private OrderStatus status;
    private Money totalAmount;
    private Address shippingAddress;
    private LocalDateTime placedAt;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;

    public Order(OrderId id, String orderNumber, CustomerId customerId, OrderStatus status,
                 Money totalAmount, Address shippingAddress, List<OrderLine> lines,
                 LocalDateTime placedAt, LocalDateTime paidAt, LocalDateTime shippedAt,
                 LocalDateTime deliveredAt, LocalDateTime cancelledAt, String cancellationReason) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
        if (lines != null) {
            this.lines.addAll(lines);
        }
        this.placedAt = placedAt;
        this.paidAt = paidAt;
        this.shippedAt = shippedAt;
        this.deliveredAt = deliveredAt;
        this.cancelledAt = cancelledAt;
        this.cancellationReason = cancellationReason;
    }

    public static Order create(String orderNumber, CustomerId customerId, String currency) {
        return new Order(OrderId.generate(), orderNumber, customerId, OrderStatus.DRAFT,
                Money.zero(currency), null, null,
                null, null, null, null, null, null);
    }

    public static Order reconstitute(OrderId id, String orderNumber, CustomerId customerId,
                                      OrderStatus status, Money totalAmount, Address shippingAddress,
                                      List<OrderLine> lines, LocalDateTime placedAt, LocalDateTime paidAt,
                                      LocalDateTime shippedAt, LocalDateTime deliveredAt,
                                      LocalDateTime cancelledAt, String cancellationReason) {
        return new Order(id, orderNumber, customerId, status, totalAmount, shippingAddress,
                lines, placedAt, paidAt, shippedAt, deliveredAt, cancelledAt, cancellationReason);
    }

    public void addLine(OrderLine line) {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Cannot add lines to a non-DRAFT order");
        }
        lines.add(line);
        recalculateTotal();
    }

    public OrderPlacedEvent place(Address shippingAddress) {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Order can only be placed from DRAFT status");
        }
        if (lines.isEmpty()) {
            throw new IllegalStateException("Cannot place an order with no lines");
        }
        this.shippingAddress = shippingAddress;
        this.status = OrderStatus.PLACED;
        this.placedAt = LocalDateTime.now();
        return OrderPlacedEvent.now(id, customerId, totalAmount);
    }

    public void markPaid() {
        if (status != OrderStatus.PLACED) {
            throw new IllegalStateException("Order must be PLACED to mark as paid");
        }
        this.status = OrderStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    public void markShipped() {
        if (status != OrderStatus.PAID) {
            throw new IllegalStateException("Order must be PAID to mark as shipped");
        }
        this.status = OrderStatus.SHIPPED;
        this.shippedAt = LocalDateTime.now();
    }

    public void markDelivered() {
        if (status != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Order must be SHIPPED to mark as delivered");
        }
        this.status = OrderStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    public void cancel(String reason) {
        if (status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel an order that has been shipped or delivered");
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;
    }

    private void recalculateTotal() {
        Money total = Money.zero(totalAmount.currency());
        for (OrderLine line : lines) {
            total = total.add(line.getLineTotal());
        }
        this.totalAmount = total;
    }

    public OrderId getId() { return id; }
    public void setId(OrderId id) { this.id = id; }
    public String getOrderNumber() { return orderNumber; }
    public CustomerId getCustomerId() { return customerId; }
    public List<OrderLine> getLines() { return Collections.unmodifiableList(lines); }
    public OrderStatus getStatus() { return status; }
    public Money getTotalAmount() { return totalAmount; }
    public Address getShippingAddress() { return shippingAddress; }
    public LocalDateTime getPlacedAt() { return placedAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public LocalDateTime getShippedAt() { return shippedAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public String getCancellationReason() { return cancellationReason; }
}
