package com.acme.shop.controller;

import com.acme.shop.model.Shipment;
import com.acme.shop.service.ShippingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shipments")
public class ShippingController {

    private final ShippingService shippingService;

    public ShippingController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @PostMapping
    public ResponseEntity<Shipment> createShipment(
            @RequestParam Long orderId, @RequestParam(defaultValue = "DHL") String carrier) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shippingService.createShipment(orderId, carrier));
    }

    @PostMapping("/{trackingNumber}/ship")
    public ResponseEntity<Shipment> shipOrder(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(shippingService.shipOrder(trackingNumber));
    }

    @PostMapping("/{trackingNumber}/deliver")
    public ResponseEntity<Shipment> markDelivered(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(shippingService.markDelivered(trackingNumber));
    }
}
