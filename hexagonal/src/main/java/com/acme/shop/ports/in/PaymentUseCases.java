package com.acme.shop.ports.in;

import com.acme.shop.domain.order.OrderId;
import com.acme.shop.domain.payment.Payment;

public interface PaymentUseCases {
    Payment processPayment(OrderId orderId, String paymentMethod);
    Payment capturePayment(String paymentReference);
}
