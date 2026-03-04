package com.acme.shop.infrastructure.external;

import com.acme.shop.domain.customer.Email;
import com.acme.shop.domain.order.Money;
import com.acme.shop.ports.out.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationAdapter implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(NotificationAdapter.class);

    @Override
    public void sendOrderConfirmation(Email email, String orderNumber, Money totalAmount) {
        log.info("Sending order confirmation to {}: Order {} - Total: {} {}",
                email.value(), orderNumber, totalAmount.amount(), totalAmount.currency());
    }

    @Override
    public void sendShipmentNotification(Email email, String trackingNumber, String carrier) {
        log.info("Sending shipment notification to {}: Tracking {} via {}",
                email.value(), trackingNumber, carrier);
    }
}
