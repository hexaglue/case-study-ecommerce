package com.acme.shop.ports.out;

import com.acme.shop.domain.order.OrderId;
import com.acme.shop.domain.shipping.Shipment;
import com.acme.shop.domain.shipping.ShipmentId;
import java.util.List;
import java.util.Optional;

public interface ShipmentRepository {
    Shipment save(Shipment shipment);
    Optional<Shipment> findById(ShipmentId id);
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    List<Shipment> findByOrderId(OrderId orderId);
}
