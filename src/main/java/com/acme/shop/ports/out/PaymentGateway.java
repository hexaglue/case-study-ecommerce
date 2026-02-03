package com.acme.shop.ports.out;

import com.acme.shop.domain.order.Money;

public interface PaymentGateway {
    boolean authorize(String paymentReference, Money amount, String paymentMethod);
    boolean capture(String paymentReference, Money amount);
    boolean refund(String paymentReference, Money amount);
}
