package com.acme.shop.application;

import com.acme.shop.domain.order.Order;
import com.acme.shop.domain.order.OrderStatus;
import com.acme.shop.domain.shipping.Shipment;
import com.acme.shop.ports.in.InventoryUseCases;
import com.acme.shop.ports.in.ShippingUseCases;
import com.acme.shop.ports.out.OrderRepository;
import com.acme.shop.ports.out.ShipmentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShippingApplicationService implements ShippingUseCases {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final InventoryUseCases inventoryUseCases;

    public ShippingApplicationService(
            ShipmentRepository shipmentRepository,
            OrderRepository orderRepository,
            InventoryUseCases inventoryUseCases) {
        this.shipmentRepository = shipmentRepository;
        this.orderRepository = orderRepository;
        this.inventoryUseCases = inventoryUseCases;
    }

    @Override
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

    @Override
    public Shipment shipOrder(String trackingNumber) {
        Shipment shipment = shipmentRepository
                .findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + trackingNumber));

        if (shipment.getStatus() != Shipment.ShipmentStatus.PENDING) {
            throw new IllegalStateException("Shipment must be PENDING to ship");
        }

        shipment.getOrder().getLines().forEach(line ->
                inventoryUseCases.shipStock(line.getProduct().getId(), line.getQuantity()));

        shipment.setStatus(Shipment.ShipmentStatus.IN_TRANSIT);
        shipment.setShippedAt(LocalDateTime.now());

        Order order = shipment.getOrder();
        order.setStatus(OrderStatus.SHIPPED);
        order.setShippedAt(LocalDateTime.now());
        orderRepository.save(order);

        return shipmentRepository.save(shipment);
    }

    @Override
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

    private BigDecimal calculateShippingCost(Order order) {
        return BigDecimal.valueOf(5.99).multiply(BigDecimal.valueOf(order.getLines().size()));
    }
}
