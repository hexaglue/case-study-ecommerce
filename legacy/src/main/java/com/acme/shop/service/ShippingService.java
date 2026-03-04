package com.acme.shop.service;

import com.acme.shop.model.Order;
import com.acme.shop.model.OrderStatus;
import com.acme.shop.model.Shipment;
import com.acme.shop.repository.OrderRepository;
import com.acme.shop.repository.ShipmentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShippingService {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    public ShippingService(
            ShipmentRepository shipmentRepository,
            OrderRepository orderRepository,
            InventoryService inventoryService) {
        this.shipmentRepository = shipmentRepository;
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
    }

    public Shipment createShipment(Long orderId, String carrier) {
        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Order must be PAID to create shipment");
        }

        Shipment shipment = new Shipment();
        shipment.setTrackingNumber("TRACK-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
        shipment.setOrder(order);
        shipment.setCarrier(carrier);
        shipment.setStatus(Shipment.ShipmentStatus.PENDING);
        shipment.setShippingCost(calculateShippingCost(order));
        shipment.setCurrency(order.getCurrency());
        shipment.setDestinationStreet(order.getShippingStreet());
        shipment.setDestinationCity(order.getShippingCity());
        shipment.setDestinationZipCode(order.getShippingZipCode());
        shipment.setDestinationCountry(order.getShippingCountry());

        return shipmentRepository.save(shipment);
    }

    public Shipment shipOrder(String trackingNumber) {
        Shipment shipment = shipmentRepository
                .findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + trackingNumber));

        if (shipment.getStatus() != Shipment.ShipmentStatus.PENDING) {
            throw new IllegalStateException("Shipment must be PENDING to ship");
        }

        // Reduce stock for each order line
        shipment.getOrder().getLines().forEach(line -> inventoryService.shipStock(line.getProduct().getId(), line.getQuantity()));

        shipment.setStatus(Shipment.ShipmentStatus.IN_TRANSIT);
        shipment.setShippedAt(LocalDateTime.now());

        Order order = shipment.getOrder();
        order.setStatus(OrderStatus.SHIPPED);
        order.setShippedAt(LocalDateTime.now());
        orderRepository.save(order);

        return shipmentRepository.save(shipment);
    }

    public Shipment markDelivered(String trackingNumber) {
        Shipment shipment = shipmentRepository
                .findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + trackingNumber));

        shipment.setStatus(Shipment.ShipmentStatus.DELIVERED);
        shipment.setDeliveredAt(LocalDateTime.now());

        Order order = shipment.getOrder();
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        orderRepository.save(order);

        return shipmentRepository.save(shipment);
    }

    // Anti-pattern: shipping cost as simple BigDecimal, should be a value object
    private BigDecimal calculateShippingCost(Order order) {
        // Simplified: flat rate per line
        return BigDecimal.valueOf(5.99).multiply(BigDecimal.valueOf(order.getLines().size()));
    }
}
