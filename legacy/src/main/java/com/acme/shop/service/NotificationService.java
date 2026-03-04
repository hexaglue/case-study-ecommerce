package com.acme.shop.service;

import com.acme.shop.event.OrderCreatedEvent;
import com.acme.shop.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Anti-pattern: @Service for infrastructure concern (notifications).
 * Should be a driven adapter behind a NotificationSender port.
 * Also listens to Spring events (framework coupling).
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        Order order = event.getOrder();
        sendEmail(
                order.getCustomer().getEmail(),
                "Order Confirmation - " + order.getOrderNumber(),
                "Thank you for your order " + order.getOrderNumber() + "! Total: "
                        + order.getTotalAmount() + " " + order.getCurrency());
    }

    public void sendEmail(String to, String subject, String body) {
        // Simulated email sending
        log.info("Sending email to {}: {} - {}", to, subject, body);
    }

    public void sendShipmentNotification(String email, String trackingNumber, String carrier) {
        sendEmail(
                email,
                "Your order has been shipped!",
                "Tracking: " + trackingNumber + " via " + carrier);
    }
}
