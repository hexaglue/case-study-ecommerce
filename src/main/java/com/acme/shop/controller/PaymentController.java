package com.acme.shop.controller;

import com.acme.shop.dto.PaymentRequest;
import com.acme.shop.model.Payment;
import com.acme.shop.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Payment> processPayment(@Valid @RequestBody PaymentRequest request) {
        Payment payment = paymentService.processPayment(request.orderId(), request.paymentMethod());
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/capture")
    public ResponseEntity<Payment> capturePayment(@RequestParam String paymentReference) {
        return ResponseEntity.ok(paymentService.capturePayment(paymentReference));
    }
}
