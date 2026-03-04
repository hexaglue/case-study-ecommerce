package com.acme.shop.ports.out;

import com.acme.shop.domain.customer.CustomerId;
import com.acme.shop.domain.order.Order;
import com.acme.shop.domain.order.OrderId;
import com.acme.shop.domain.order.OrderStatus;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByCustomerId(CustomerId customerId);
    List<Order> findByStatus(OrderStatus status);
}
