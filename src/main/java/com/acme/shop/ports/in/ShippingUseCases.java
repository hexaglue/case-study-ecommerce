package com.acme.shop.ports.in;

import com.acme.shop.domain.shipping.Shipment;

public interface ShippingUseCases {
    Shipment createShipment(Long orderId, String carrier);
    Shipment shipOrder(String trackingNumber);
    Shipment markDelivered(String trackingNumber);
}
