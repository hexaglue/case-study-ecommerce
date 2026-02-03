package com.acme.shop.application;

import com.acme.shop.domain.order.Order;
import com.acme.shop.domain.order.OrderId;
import com.acme.shop.domain.order.OrderStatus;
import com.acme.shop.domain.payment.Payment;
import com.acme.shop.ports.in.PaymentUseCases;
import com.acme.shop.ports.out.OrderRepository;
import com.acme.shop.ports.out.PaymentGateway;
import com.acme.shop.ports.out.PaymentRepository;
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
    public Payment processPayment(OrderId orderId, String paymentMethod) {
        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new IllegalStateException("Order must be in PLACED status to process payment");
        }

        String paymentReference = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Payment payment = Payment.create(paymentReference, orderId, order.getTotalAmount(), paymentMethod);
        Payment saved = paymentRepository.save(payment);

        boolean success = paymentGateway.authorize(
                saved.getPaymentReference(), order.getTotalAmount(), paymentMethod);

        if (success) {
            String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
            saved.authorize(transactionId);

            order.markPaid();
            orderRepository.save(order);
        } else {
            saved.fail("Authorization declined");
        }

        return paymentRepository.save(saved);
    }

    @Override
    public Payment capturePayment(String paymentReference) {
        Payment payment = paymentRepository
                .findByPaymentReference(paymentReference)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentReference));

        boolean success = paymentGateway.capture(paymentReference, payment.getAmount());
        if (success) {
            payment.capture();
        } else {
            payment.fail("Capture failed");
        }

        return paymentRepository.save(payment);
    }
}
