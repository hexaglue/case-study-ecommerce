package com.acme.shop.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ShipmentResponse(
        UUID id,
        String trackingNumber,
        UUID orderId,
        String carrier,
        String status,
        BigDecimal shippingCost,
        String currency,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt) {}
