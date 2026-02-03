package com.acme.shop.ports.out;

import com.acme.shop.domain.payment.Payment;
import com.acme.shop.domain.payment.PaymentStatus;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(Long id);
    Optional<Payment> findByPaymentReference(String paymentReference);
    List<Payment> findByOrderId(Long orderId);
    List<Payment> findByStatus(PaymentStatus status);
}
