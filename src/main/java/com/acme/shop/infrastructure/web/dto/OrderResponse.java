package com.acme.shop.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        UUID customerId,
        String customerName,
        List<LineItemResponse> lines,
        String status,
        BigDecimal totalAmount,
        String currency,
        LocalDateTime placedAt,
        LocalDateTime createdAt) {

    public record LineItemResponse(
            UUID productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal) {}
}
