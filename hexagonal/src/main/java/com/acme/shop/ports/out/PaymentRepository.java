package com.acme.shop.ports.out;

import com.acme.shop.domain.order.OrderId;
import com.acme.shop.domain.payment.Payment;
import com.acme.shop.domain.payment.PaymentId;
import com.acme.shop.domain.payment.PaymentStatus;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(PaymentId id);
    Optional<Payment> findByPaymentReference(String paymentReference);
    List<Payment> findByOrderId(OrderId orderId);
    List<Payment> findByStatus(PaymentStatus status);
}
