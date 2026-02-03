package com.acme.shop.infrastructure.web;

import com.acme.shop.domain.order.OrderId;
import com.acme.shop.domain.payment.Payment;
import com.acme.shop.infrastructure.web.dto.PaymentRequest;
import com.acme.shop.infrastructure.web.dto.PaymentResponse;
import com.acme.shop.ports.in.PaymentUseCases;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentUseCases paymentUseCases;

    public PaymentController(PaymentUseCases paymentUseCases) {
        this.paymentUseCases = paymentUseCases;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        Payment payment = paymentUseCases.processPayment(
                new OrderId(request.orderId()), request.paymentMethod());
        return ResponseEntity.ok(toResponse(payment));
    }

    @PostMapping("/capture")
    public ResponseEntity<PaymentResponse> capturePayment(@RequestParam String paymentReference) {
        return ResponseEntity.ok(toResponse(paymentUseCases.capturePayment(paymentReference)));
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId() != null ? payment.getId().value() : null,
                payment.getPaymentReference(),
                payment.getOrderId() != null ? payment.getOrderId().value() : null,
                payment.getAmount().amount(),
                payment.getAmount().currency(),
                payment.getPaymentMethod(),
                payment.getStatus().name(),
                payment.getTransactionId(),
                payment.getAuthorizedAt(),
                payment.getCapturedAt());
    }
}
