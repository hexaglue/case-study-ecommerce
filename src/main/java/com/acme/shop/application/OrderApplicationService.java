package com.acme.shop.application;

import com.acme.shop.domain.customer.Customer;
import com.acme.shop.domain.customer.CustomerId;
import com.acme.shop.domain.inventory.Inventory;
import com.acme.shop.domain.order.Address;
import com.acme.shop.domain.order.Order;
import com.acme.shop.domain.order.OrderId;
import com.acme.shop.domain.order.OrderLine;
import com.acme.shop.domain.order.Quantity;
import com.acme.shop.domain.product.Product;
import com.acme.shop.exception.OrderNotFoundException;
import com.acme.shop.ports.in.OrderUseCases;
import com.acme.shop.ports.out.CustomerRepository;
import com.acme.shop.ports.out.InventoryRepository;
import com.acme.shop.ports.out.NotificationSender;
import com.acme.shop.ports.out.OrderRepository;
import com.acme.shop.ports.out.ProductRepository;
import java.util.List;
import java.util.UUID;

public class OrderApplicationService implements OrderUseCases {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final NotificationSender notificationSender;

    public OrderApplicationService(
            OrderRepository orderRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            NotificationSender notificationSender) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
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

            Inventory inventory = inventoryRepository
                    .findByProductId(product.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "No inventory record for product: " + product.getId()));
            inventory.reserve(item.quantity());
            inventoryRepository.save(inventory);

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
            Inventory inventory = inventoryRepository
                    .findByProductId(line.getProductId())
                    .orElseThrow(() -> new IllegalStateException(
                            "No inventory record for product: " + line.getProductId()));
            inventory.release(line.getQuantity().value());
            inventoryRepository.save(inventory);
        }

        order.cancel(reason);
        return orderRepository.save(order);
    }

    @Override
    public Order getOrder(OrderId orderId) {
        return findOrderOrThrow(orderId);
    }

    @Override
    public Order getOrderByNumber(String orderNumber) {
        return orderRepository
                .findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderNumber));
    }

    @Override
    public List<Order> getOrdersByCustomer(CustomerId customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    private Order findOrderOrThrow(OrderId orderId) {
        return orderRepository
                .findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }
}
