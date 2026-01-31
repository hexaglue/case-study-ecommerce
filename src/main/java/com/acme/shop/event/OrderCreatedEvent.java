package com.acme.shop.event;

import com.acme.shop.model.Order;
import org.springframework.context.ApplicationEvent;

/**
 * Anti-pattern: Spring ApplicationEvent instead of a domain event.
 * Couples domain logic to Spring framework.
 */
public class OrderCreatedEvent extends ApplicationEvent {

    private final Order order;

    public OrderCreatedEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }
}
