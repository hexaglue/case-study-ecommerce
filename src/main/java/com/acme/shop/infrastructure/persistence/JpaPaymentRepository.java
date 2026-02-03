package com.acme.shop.infrastructure.persistence;

import com.acme.shop.domain.payment.Payment;
import com.acme.shop.ports.out.PaymentRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaPaymentRepository extends JpaRepository<Payment, Long>, PaymentRepository {
}
