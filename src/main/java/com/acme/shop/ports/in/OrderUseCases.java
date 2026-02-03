package com.acme.shop.ports.in;

import com.acme.shop.infrastructure.web.dto.CreateOrderRequest;
import com.acme.shop.infrastructure.web.dto.OrderResponse;
import java.util.List;

public interface OrderUseCases {
    OrderResponse createOrder(CreateOrderRequest request);
    OrderResponse placeOrder(Long orderId);
    OrderResponse cancelOrder(Long orderId, String reason);
    OrderResponse getOrder(Long orderId);
    OrderResponse getOrderByNumber(String orderNumber);
    List<OrderResponse> getOrdersByCustomer(Long customerId);
}
