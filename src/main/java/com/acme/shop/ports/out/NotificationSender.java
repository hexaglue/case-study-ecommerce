package com.acme.shop.ports.out;

public interface NotificationSender {
    void sendOrderConfirmation(String email, String orderNumber, String totalAmount, String currency);
    void sendShipmentNotification(String email, String trackingNumber, String carrier);
}
