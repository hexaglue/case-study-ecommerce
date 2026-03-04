package com.acme.shop.service;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Anti-pattern: External gateway call as @Service (no port/adapter separation).
 * This should be a driven port interface with an infrastructure adapter.
 * Simulates a payment gateway for the case study.
 */
@Service
public class PaymentGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayClient.class);

    public boolean authorize(String paymentReference, BigDecimal amount, String currency, String paymentMethod) {
        log.info("Authorizing payment {} for {} {} via {}", paymentReference, amount, currency, paymentMethod);
        // Simulate gateway call - always succeeds for demo
        return true;
    }

    public boolean capture(String paymentReference, BigDecimal amount) {
        log.info("Capturing payment {} for {}", paymentReference, amount);
        return true;
    }

    public boolean refund(String paymentReference, BigDecimal amount) {
        log.info("Refunding payment {} for {}", paymentReference, amount);
        return true;
    }
}
