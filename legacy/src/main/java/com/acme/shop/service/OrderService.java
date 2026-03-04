package com.acme.shop.service;

import com.acme.shop.dto.CreateOrderRequest;
import com.acme.shop.dto.OrderResponse;
import com.acme.shop.event.OrderCreatedEvent;
import com.acme.shop.exception.OrderNotFoundException;
import com.acme.shop.model.Customer;
import com.acme.shop.model.Order;
import com.acme.shop.model.OrderLine;
import com.acme.shop.model.OrderStatus;
import com.acme.shop.model.Product;
import com.acme.shop.repository.CustomerRepository;
import com.acme.shop.repository.OrderRepository;
import com.acme.shop.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anti-patterns:
 * - @Service with all business logic (anemic domain model)
 * - Direct dependency on Spring Data repositories (no port)
 * - DTO conversion mixed with business logic
 * - Order state transitions managed here instead of in the Order entity
 * - ApplicationEventPublisher dependency
 */
@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(
            OrderRepository orderRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            InventoryService inventoryService,
            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.eventPublisher = eventPublisher;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        Customer customer = customerRepository
                .findById(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + request.customerId()));

        Order order = new Order();
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);
        order.setCurrency("EUR");

        // Anti-pattern: business logic in service instead of Order aggregate
        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderRequest.LineItem item : request.items()) {
            Product product = productRepository
                    .findById(item.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.productId()));

            if (!product.isActive()) {
                throw new IllegalArgumentException("Product is not active: " + product.getName());
            }

            inventoryService.reserveStock(product.getId(), item.quantity());

            OrderLine line = new OrderLine(order, product, item.quantity(), product.getPrice());
            order.getLines().add(line);
            total = total.add(line.getLineTotal());
        }

        order.setTotalAmount(total);

        // Anti-pattern: shipping address as flat strings
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

        Order saved = orderRepository.save(order);

        return toResponse(saved);
    }

    public OrderResponse placeOrder(Long orderId) {
        Order order = findOrderOrThrow(orderId);

        // Anti-pattern: state transition logic in service
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new IllegalStateException("Order can only be placed from DRAFT status");
        }
        if (order.getLines().isEmpty()) {
            throw new IllegalStateException("Cannot place an order with no lines");
        }

        order.setStatus(OrderStatus.PLACED);
        order.setPlacedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);

        // Anti-pattern: Spring event instead of domain event
        eventPublisher.publishEvent(new OrderCreatedEvent(this, saved));

        return toResponse(saved);
    }

    public OrderResponse cancelOrder(Long orderId, String reason) {
        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel an order that has been shipped or delivered");
        }

        // Anti-pattern: releasing stock logic in service
        for (OrderLine line : order.getLines()) {
            inventoryService.releaseStock(line.getProduct().getId(), line.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(reason);

        return toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        return toResponse(findOrderOrThrow(orderId));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository
                .findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderNumber));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    // Anti-pattern: DTO conversion in service
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
