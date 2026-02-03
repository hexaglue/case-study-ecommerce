package com.acme.shop.ports.out;

import java.math.BigDecimal;

public interface PaymentGateway {
    boolean authorize(String paymentReference, BigDecimal amount, String currency, String paymentMethod);
    boolean capture(String paymentReference, BigDecimal amount);
    boolean refund(String paymentReference, BigDecimal amount);
}
