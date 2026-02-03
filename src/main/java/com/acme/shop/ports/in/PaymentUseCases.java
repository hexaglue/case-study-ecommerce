package com.acme.shop.ports.in;

import com.acme.shop.domain.payment.Payment;

public interface PaymentUseCases {
    Payment processPayment(Long orderId, String paymentMethod);
    Payment capturePayment(String paymentReference);
}
