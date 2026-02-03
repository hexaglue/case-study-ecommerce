package com.acme.shop.domain.order;

import com.acme.shop.domain.customer.CustomerId;
import java.time.Instant;

public record OrderPlacedEvent(
        OrderId orderId,
        CustomerId customerId,
        Money totalAmount,
        Instant occurredAt) {

    public static OrderPlacedEvent now(OrderId orderId, CustomerId customerId, Money totalAmount) {
        return new OrderPlacedEvent(orderId, customerId, totalAmount, Instant.now());
    }
}
