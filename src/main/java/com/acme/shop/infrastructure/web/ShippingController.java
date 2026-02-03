package com.acme.shop.infrastructure.web;

import com.acme.shop.domain.order.OrderId;
import com.acme.shop.domain.shipping.Shipment;
import com.acme.shop.ports.in.ShippingUseCases;
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
    public ResponseEntity<Shipment> createShipment(
            @RequestParam Long orderId, @RequestParam(defaultValue = "DHL") String carrier) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shippingUseCases.createShipment(new OrderId(orderId), carrier));
    }

    @PostMapping("/{trackingNumber}/ship")
    public ResponseEntity<Shipment> shipOrder(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(shippingUseCases.shipOrder(trackingNumber));
    }

    @PostMapping("/{trackingNumber}/deliver")
    public ResponseEntity<Shipment> markDelivered(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(shippingUseCases.markDelivered(trackingNumber));
    }
}
