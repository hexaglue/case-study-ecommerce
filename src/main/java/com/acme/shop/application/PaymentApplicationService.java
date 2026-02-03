package com.acme.shop.application;

import com.acme.shop.domain.order.Order;
import com.acme.shop.domain.order.OrderStatus;
import com.acme.shop.domain.payment.Payment;
import com.acme.shop.domain.payment.PaymentStatus;
import com.acme.shop.ports.in.PaymentUseCases;
import com.acme.shop.ports.out.OrderRepository;
import com.acme.shop.ports.out.PaymentGateway;
import com.acme.shop.ports.out.PaymentRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentApplicationService implements PaymentUseCases {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;

    public PaymentApplicationService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            PaymentGateway paymentGateway) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
    }

    @Override
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

        boolean success = paymentGateway.authorize(
                saved.getPaymentReference(), order.getTotalAmount(), order.getCurrency(), paymentMethod);

        if (success) {
            saved.setStatus(PaymentStatus.AUTHORIZED);
            saved.setAuthorizedAt(LocalDateTime.now());
            saved.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());

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

    @Override
    public Payment capturePayment(String paymentReference) {
        Payment payment = paymentRepository
                .findByPaymentReference(paymentReference)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentReference));

        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Payment must be AUTHORIZED to capture");
        }

        boolean success = paymentGateway.capture(paymentReference, payment.getAmount());
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
