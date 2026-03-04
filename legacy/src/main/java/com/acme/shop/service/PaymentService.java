package com.acme.shop.service;

import com.acme.shop.model.Order;
import com.acme.shop.model.OrderStatus;
import com.acme.shop.model.Payment;
import com.acme.shop.model.PaymentStatus;
import com.acme.shop.repository.OrderRepository;
import com.acme.shop.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anti-patterns:
 * - Direct dependency on repositories (no ports)
 * - Payment gateway call simulated inline (no port)
 * - Manages Order state transitions
 */
@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentGatewayClient paymentGatewayClient;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            PaymentGatewayClient paymentGatewayClient) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.paymentGatewayClient = paymentGatewayClient;
    }

    public Payment processPayment(Long orderId, String paymentMethod) {
        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new IllegalStateException("Order must be in PLACED status to process payment");
        }

        Payment payment = new Payment();
        payment.setPaymentReference("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setCurrency(order.getCurrency());
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(PaymentStatus.PENDING);

        Payment saved = paymentRepository.save(payment);

        // Anti-pattern: external call without port abstraction
        boolean success = paymentGatewayClient.authorize(
                saved.getPaymentReference(), order.getTotalAmount(), order.getCurrency(), paymentMethod);

        if (success) {
            saved.setStatus(PaymentStatus.AUTHORIZED);
            saved.setAuthorizedAt(LocalDateTime.now());
            saved.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());

            // Anti-pattern: service manages Order state
            order.setStatus(OrderStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
            orderRepository.save(order);
        } else {
            saved.setStatus(PaymentStatus.FAILED);
            saved.setFailedAt(LocalDateTime.now());
            saved.setFailureReason("Authorization declined");
        }

        return paymentRepository.save(saved);
    }

    public Payment capturePayment(String paymentReference) {
        Payment payment = paymentRepository
                .findByPaymentReference(paymentReference)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentReference));

        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Payment must be AUTHORIZED to capture");
        }

        boolean success = paymentGatewayClient.capture(paymentReference, payment.getAmount());
        if (success) {
            payment.setStatus(PaymentStatus.CAPTURED);
            payment.setCapturedAt(LocalDateTime.now());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailedAt(LocalDateTime.now());
            payment.setFailureReason("Capture failed");
        }

        return paymentRepository.save(payment);
    }
}
