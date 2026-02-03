package com.acme.shop.infrastructure.persistence;

import com.acme.shop.domain.order.Order;
import com.acme.shop.ports.out.OrderRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaOrderRepository extends JpaRepository<Order, Long>, OrderRepository {
}
