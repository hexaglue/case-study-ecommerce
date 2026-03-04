package com.acme.shop.repository;

import com.acme.shop.model.Order;
import com.acme.shop.model.OrderStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Anti-pattern: Spring Data repository directly used by services,
 * no domain port interface.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByCustomerId(Long customerId);

    List<Order> findByStatus(OrderStatus status);
}
