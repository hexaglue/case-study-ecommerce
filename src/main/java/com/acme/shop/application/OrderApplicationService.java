package com.acme.shop.application;

import com.acme.shop.domain.customer.Customer;
import com.acme.shop.domain.customer.CustomerId;
import com.acme.shop.domain.order.Address;
import com.acme.shop.domain.order.Order;
import com.acme.shop.domain.order.OrderId;
import com.acme.shop.domain.order.OrderLine;
import com.acme.shop.domain.order.Quantity;
import com.acme.shop.domain.product.Product;
import com.acme.shop.exception.OrderNotFoundException;
import com.acme.shop.ports.in.InventoryUseCases;
import com.acme.shop.ports.in.OrderUseCases;
import com.acme.shop.ports.out.CustomerRepository;
import com.acme.shop.ports.out.NotificationSender;
import com.acme.shop.ports.out.OrderRepository;
import com.acme.shop.ports.out.ProductRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderApplicationService implements OrderUseCases {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryUseCases inventoryUseCases;
    private final NotificationSender notificationSender;

    public OrderApplicationService(
            OrderRepository orderRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            InventoryUseCases inventoryUseCases,
            NotificationSender notificationSender) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.inventoryUseCases = inventoryUseCases;
        this.notificationSender = notificationSender;
    }

    @Override
    public Order createOrder(CustomerId customerId, List<LineItemCommand> items, Address shippingAddress) {
        Customer customer = customerRepository
                .findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = Order.create(orderNumber, customerId, "EUR");

        for (LineItemCommand item : items) {
            Product product = productRepository
                    .findById(item.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.productId()));

            if (!product.isActive()) {
                throw new IllegalArgumentException("Product is not active: " + product.getName());
            }

            inventoryUseCases.reserveStock(product.getId(), item.quantity());

            OrderLine line = OrderLine.create(
                    product.getId(),
                    product.getName(),
                    Quantity.of(item.quantity()),
                    product.getPrice());
            order.addLine(line);
        }

        Address address = shippingAddress != null ? shippingAddress :
                customer.getAddress();
        order.place(address);

        return orderRepository.save(order);
    }

    @Override
    public Order placeOrder(OrderId orderId) {
        Order order = findOrderOrThrow(orderId);
        Customer customer = customerRepository
                .findById(order.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        Address address = order.getShippingAddress() != null ? order.getShippingAddress() :
                customer.getAddress();

        order.place(address);
        Order saved = orderRepository.save(order);

        notificationSender.sendOrderConfirmation(
                customer.getEmail(),
                saved.getOrderNumber(),
                saved.getTotalAmount());

        return saved;
    }

    @Override
    public Order cancelOrder(OrderId orderId, String reason) {
        Order order = findOrderOrThrow(orderId);

        for (OrderLine line : order.getLines()) {
            inventoryUseCases.releaseStock(line.getProductId(), line.getQuantity().value());
        }

        order.cancel(reason);
        return orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrder(OrderId orderId) {
        return findOrderOrThrow(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderByNumber(String orderNumber) {
        return orderRepository
                .findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomer(CustomerId customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    private Order findOrderOrThrow(OrderId orderId) {
        return orderRepository
                .findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }
}
