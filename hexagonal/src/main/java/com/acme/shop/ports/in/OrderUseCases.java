package com.acme.shop.ports.in;

import com.acme.shop.domain.customer.CustomerId;
import com.acme.shop.domain.order.Address;
import com.acme.shop.domain.order.Order;
import com.acme.shop.domain.order.OrderId;
import com.acme.shop.domain.product.ProductId;
import java.util.List;

public interface OrderUseCases {
    Order createOrder(CustomerId customerId, List<LineItemCommand> items, Address shippingAddress);
    Order placeOrder(OrderId orderId);
    Order cancelOrder(OrderId orderId, String reason);
    Order getOrder(OrderId orderId);
    Order getOrderByNumber(String orderNumber);
    List<Order> getOrdersByCustomer(CustomerId customerId);

    record LineItemCommand(ProductId productId, int quantity) {}
}
