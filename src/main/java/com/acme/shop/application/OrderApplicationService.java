package com.acme.shop.application;

import com.acme.shop.domain.customer.Customer;
import com.acme.shop.domain.order.Order;
import com.acme.shop.domain.order.OrderLine;
import com.acme.shop.domain.order.OrderStatus;
import com.acme.shop.domain.product.Product;
import com.acme.shop.exception.OrderNotFoundException;
import com.acme.shop.infrastructure.web.dto.CreateOrderRequest;
import com.acme.shop.infrastructure.web.dto.OrderResponse;
import com.acme.shop.ports.in.InventoryUseCases;
import com.acme.shop.ports.in.OrderUseCases;
import com.acme.shop.ports.out.CustomerRepository;
import com.acme.shop.ports.out.NotificationSender;
import com.acme.shop.ports.out.OrderRepository;
import com.acme.shop.ports.out.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    public OrderResponse createOrder(CreateOrderRequest request) {
        Customer customer = customerRepository
                .findById(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + request.customerId()));

        Order order = new Order();
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);
        order.setCurrency("EUR");

        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderRequest.LineItem item : request.items()) {
            Product product = productRepository
                    .findById(item.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.productId()));

            if (!product.isActive()) {
                throw new IllegalArgumentException("Product is not active: " + product.getName());
            }

            inventoryUseCases.reserveStock(product.getId(), item.quantity());

            OrderLine line = new OrderLine(order, product, item.quantity(), product.getPrice());
            order.getLines().add(line);
            total = total.add(line.getLineTotal());
        }

        order.setTotalAmount(total);

        if (request.shippingStreet() != null) {
            order.setShippingStreet(request.shippingStreet());
            order.setShippingCity(request.shippingCity());
            order.setShippingZipCode(request.shippingZipCode());
            order.setShippingCountry(request.shippingCountry());
        } else {
            order.setShippingStreet(customer.getStreet());
            order.setShippingCity(customer.getCity());
            order.setShippingZipCode(customer.getZipCode());
            order.setShippingCountry(customer.getCountry());
        }

        return toResponse(orderRepository.save(order));
    }

    @Override
    public OrderResponse placeOrder(Long orderId) {
        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new IllegalStateException("Order can only be placed from DRAFT status");
        }
        if (order.getLines().isEmpty()) {
            throw new IllegalStateException("Cannot place an order with no lines");
        }

        order.setStatus(OrderStatus.PLACED);
        order.setPlacedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);

        notificationSender.sendOrderConfirmation(
                saved.getCustomer().getEmail(),
                saved.getOrderNumber(),
                saved.getTotalAmount().toString(),
                saved.getCurrency());

        return toResponse(saved);
    }

    @Override
    public OrderResponse cancelOrder(Long orderId, String reason) {
        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel an order that has been shipped or delivered");
        }

        for (OrderLine line : order.getLines()) {
            inventoryUseCases.releaseStock(line.getProduct().getId(), line.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(reason);

        return toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        return toResponse(findOrderOrThrow(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository
                .findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderNumber));
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.LineItemResponse> lines = order.getLines().stream()
                .map(line -> new OrderResponse.LineItemResponse(
                        line.getProduct().getId(),
                        line.getProduct().getName(),
                        line.getQuantity(),
                        line.getUnitPrice(),
                        line.getLineTotal()))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomer().getId(),
                order.getCustomer().getFullName(),
                lines,
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getPlacedAt(),
                order.getCreatedAt());
    }

    private Order findOrderOrThrow(Long orderId) {
        return orderRepository
                .findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }
}
