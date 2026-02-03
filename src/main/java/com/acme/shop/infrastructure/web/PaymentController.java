package com.acme.shop.infrastructure.web;

import com.acme.shop.domain.payment.Payment;
import com.acme.shop.infrastructure.web.dto.PaymentRequest;
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
    public ResponseEntity<Payment> processPayment(@Valid @RequestBody PaymentRequest request) {
        Payment payment = paymentUseCases.processPayment(request.orderId(), request.paymentMethod());
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/capture")
    public ResponseEntity<Payment> capturePayment(@RequestParam String paymentReference) {
        return ResponseEntity.ok(paymentUseCases.capturePayment(paymentReference));
    }
}
