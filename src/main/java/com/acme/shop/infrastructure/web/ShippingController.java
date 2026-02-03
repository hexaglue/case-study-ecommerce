package com.acme.shop.infrastructure.web;

import com.acme.shop.domain.order.OrderId;
import com.acme.shop.domain.shipping.Shipment;
import com.acme.shop.infrastructure.web.dto.ShipmentResponse;
import com.acme.shop.ports.in.ShippingUseCases;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipments")
public class ShippingController {

    private final ShippingUseCases shippingUseCases;

    public ShippingController(ShippingUseCases shippingUseCases) {
        this.shippingUseCases = shippingUseCases;
    }

    @PostMapping
    public ResponseEntity<ShipmentResponse> createShipment(
            @RequestParam UUID orderId, @RequestParam(defaultValue = "DHL") String carrier) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(shippingUseCases.createShipment(new OrderId(orderId), carrier)));
    }

    @PostMapping("/{trackingNumber}/ship")
    public ResponseEntity<ShipmentResponse> shipOrder(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(toResponse(shippingUseCases.shipOrder(trackingNumber)));
    }

    @PostMapping("/{trackingNumber}/deliver")
    public ResponseEntity<ShipmentResponse> markDelivered(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(toResponse(shippingUseCases.markDelivered(trackingNumber)));
    }

    private ShipmentResponse toResponse(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getId() != null ? shipment.getId().value() : null,
                shipment.getTrackingNumber(),
                shipment.getOrderId() != null ? shipment.getOrderId().value() : null,
                shipment.getCarrier(),
                shipment.getStatus().name(),
                shipment.getShippingCost().amount(),
                shipment.getShippingCost().currency(),
                shipment.getShippedAt(),
                shipment.getDeliveredAt());
    }
}
