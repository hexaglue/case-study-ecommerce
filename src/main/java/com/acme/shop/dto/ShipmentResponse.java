package com.acme.shop.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShipmentResponse(
        Long id,
        String trackingNumber,
        Long orderId,
        String carrier,
        String status,
        BigDecimal shippingCost,
        String currency,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt) {}
