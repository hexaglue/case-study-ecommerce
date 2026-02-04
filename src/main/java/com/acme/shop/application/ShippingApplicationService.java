package com.acme.shop.application;

import com.acme.shop.domain.inventory.Inventory;
import com.acme.shop.domain.order.Address;
import com.acme.shop.domain.order.Money;
import com.acme.shop.domain.order.Order;
import com.acme.shop.domain.order.OrderId;
import com.acme.shop.domain.order.OrderLine;
import com.acme.shop.domain.order.OrderStatus;
import com.acme.shop.domain.shipping.Shipment;
import com.acme.shop.ports.in.ShippingUseCases;
import com.acme.shop.ports.out.InventoryRepository;
import com.acme.shop.ports.out.OrderRepository;
import com.acme.shop.ports.out.ShipmentRepository;
import java.math.BigDecimal;
import java.util.UUID;

public class ShippingApplicationService implements ShippingUseCases {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;

    public ShippingApplicationService(
            ShipmentRepository shipmentRepository,
            OrderRepository orderRepository,
            InventoryRepository inventoryRepository) {
        this.shipmentRepository = shipmentRepository;
        this.orderRepository = orderRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public Shipment createShipment(OrderId orderId, String carrier) {
        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Order must be PAID to create shipment");
        }

        String trackingNumber = "TRACK-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        Money shippingCost = Money.of(
                BigDecimal.valueOf(5.99).multiply(BigDecimal.valueOf(order.getLines().size())),
                order.getTotalAmount().currency());
        Address destination = order.getShippingAddress();

        Shipment shipment = Shipment.create(trackingNumber, orderId, carrier, shippingCost, destination);
        return shipmentRepository.save(shipment);
    }

    @Override
    public Shipment shipOrder(String trackingNumber) {
        Shipment shipment = shipmentRepository
                .findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + trackingNumber));

        Order order = orderRepository
                .findById(shipment.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Order not found for shipment"));

        for (OrderLine line : order.getLines()) {
            Inventory inventory = inventoryRepository
                    .findByProductId(line.getProductId())
                    .orElseThrow(() -> new IllegalStateException(
                            "No inventory record for product: " + line.getProductId()));
            inventory.ship(line.getQuantity().value());
            inventoryRepository.save(inventory);
        }

        shipment.ship();
        order.markShipped();
        orderRepository.save(order);

        return shipmentRepository.save(shipment);
    }

    @Override
    public Shipment markDelivered(String trackingNumber) {
        Shipment shipment = shipmentRepository
                .findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + trackingNumber));

        Order order = orderRepository
                .findById(shipment.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Order not found for shipment"));

        shipment.markDelivered();
        order.markDelivered();
        orderRepository.save(order);

        return shipmentRepository.save(shipment);
    }
}
