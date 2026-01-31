package com.acme.shop.repository;

import com.acme.shop.model.Payment;
import com.acme.shop.model.PaymentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentReference(String paymentReference);

    List<Payment> findByOrderId(Long orderId);

    List<Payment> findByStatus(PaymentStatus status);
}
