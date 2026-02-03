package com.acme.shop.infrastructure.web;

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
        return ResponseEntity.status(HttpStatus.CREATED).body(orderUseCases.createOrder(request));
    }

    @PostMapping("/{id}/place")
    public ResponseEntity<OrderResponse> placeOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderUseCases.placeOrder(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long id, @RequestParam(required = false) String reason) {
        if (reason == null || reason.isBlank()) {
            reason = "Cancelled by customer";
        }
        return ResponseEntity.ok(orderUseCases.cancelOrder(id, reason));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderUseCases.getOrder(id));
    }

    @GetMapping("/by-number/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderByNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(orderUseCases.getOrderByNumber(orderNumber));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(orderUseCases.getOrdersByCustomer(customerId));
    }
}
