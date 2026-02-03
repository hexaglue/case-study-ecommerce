package com.acme.shop.ports.out;

import com.acme.shop.domain.customer.Email;
import com.acme.shop.domain.order.Money;

public interface NotificationSender {
    void sendOrderConfirmation(Email email, String orderNumber, Money totalAmount);
    void sendShipmentNotification(Email email, String trackingNumber, String carrier);
}
