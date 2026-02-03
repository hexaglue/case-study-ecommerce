package com.acme.shop.infrastructure.external;

import com.acme.shop.ports.out.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationAdapter implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(NotificationAdapter.class);

    @Override
    public void sendOrderConfirmation(String email, String orderNumber, String totalAmount, String currency) {
        log.info("Sending order confirmation to {}: Order {} - Total: {} {}", email, orderNumber, totalAmount, currency);
    }

    @Override
    public void sendShipmentNotification(String email, String trackingNumber, String carrier) {
        log.info("Sending shipment notification to {}: Tracking {} via {}", email, trackingNumber, carrier);
    }
}
