package com.acme.shop.infrastructure.external;

import com.acme.shop.ports.out.PaymentGateway;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayAdapter implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayAdapter.class);

    @Override
    public boolean authorize(String paymentReference, BigDecimal amount, String currency, String paymentMethod) {
        log.info("Authorizing payment {} for {} {} via {}", paymentReference, amount, currency, paymentMethod);
        return true;
    }

    @Override
    public boolean capture(String paymentReference, BigDecimal amount) {
        log.info("Capturing payment {} for {}", paymentReference, amount);
        return true;
    }

    @Override
    public boolean refund(String paymentReference, BigDecimal amount) {
        log.info("Refunding payment {} for {}", paymentReference, amount);
        return true;
    }
}
