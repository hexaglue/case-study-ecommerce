package com.acme.shop.ports.in;

import com.acme.shop.domain.order.OrderId;
import com.acme.shop.domain.shipping.Shipment;

public interface ShippingUseCases {
    Shipment createShipment(OrderId orderId, String carrier);
    Shipment shipOrder(String trackingNumber);
    Shipment markDelivered(String trackingNumber);
}
