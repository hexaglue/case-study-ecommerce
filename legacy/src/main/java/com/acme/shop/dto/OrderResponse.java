package com.acme.shop.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        Long customerId,
        String customerName,
        List<LineItemResponse> lines,
        String status,
        BigDecimal totalAmount,
        String currency,
        LocalDateTime placedAt,
        LocalDateTime createdAt) {

    public record LineItemResponse(
            Long productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal) {}
}
