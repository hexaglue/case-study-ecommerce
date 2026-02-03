package com.acme.shop.infrastructure.web;

import com.acme.shop.domain.customer.CustomerId;
import com.acme.shop.domain.order.Address;
import com.acme.shop.domain.order.Order;
import com.acme.shop.domain.order.OrderId;
import com.acme.shop.domain.product.ProductId;
import com.acme.shop.infrastructure.web.dto.CreateOrderRequest;
import com.acme.shop.infrastructure.web.dto.OrderResponse;
import com.acme.shop.ports.in.OrderUseCases;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderUseCases orderUseCases;

    public OrderController(OrderUseCases orderUseCases) {
        this.orderUseCases = orderUseCases;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        List<OrderUseCases.LineItemCommand> items = request.items().stream()
                .map(item -> new OrderUseCases.LineItemCommand(
                        new ProductId(item.productId()), item.quantity()))
                .toList();

        Address shippingAddress = null;
        if (request.shippingStreet() != null) {
            shippingAddress = new Address(
                    request.shippingStreet(), request.shippingCity(),
                    request.shippingZipCode(), request.shippingCountry());
        }

        Order order = orderUseCases.createOrder(
                new CustomerId(request.customerId()), items, shippingAddress);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(order));
    }

    @PostMapping("/{id}/place")
    public ResponseEntity<OrderResponse> placeOrder(@PathVariable Long id) {
        Order order = orderUseCases.placeOrder(new OrderId(id));
        return ResponseEntity.ok(toResponse(order));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long id, @RequestParam(required = false) String reason) {
        if (reason == null || reason.isBlank()) {
            reason = "Cancelled by customer";
        }
        Order order = orderUseCases.cancelOrder(new OrderId(id), reason);
        return ResponseEntity.ok(toResponse(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        Order order = orderUseCases.getOrder(new OrderId(id));
        return ResponseEntity.ok(toResponse(order));
    }

    @GetMapping("/by-number/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderByNumber(@PathVariable String orderNumber) {
        Order order = orderUseCases.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(toResponse(order));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(@PathVariable Long customerId) {
        List<Order> orders = orderUseCases.getOrdersByCustomer(new CustomerId(customerId));
        return ResponseEntity.ok(orders.stream().map(this::toResponse).toList());
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.LineItemResponse> lines = order.getLines().stream()
                .map(line -> new OrderResponse.LineItemResponse(
                        line.getProductId().value(),
                        line.getProductName(),
                        line.getQuantity().value(),
                        line.getUnitPrice().amount(),
                        line.getLineTotal().amount()))
                .toList();

        return new OrderResponse(
                order.getId() != null ? order.getId().value() : null,
                order.getOrderNumber(),
                order.getCustomerId().value(),
                null,
                lines,
                order.getStatus().name(),
                order.getTotalAmount().amount(),
                order.getTotalAmount().currency(),
                order.getPlacedAt(),
                null);
    }
}
