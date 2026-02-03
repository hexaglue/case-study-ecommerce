package com.acme.shop.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String paymentReference,
        UUID orderId,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String status,
        String transactionId,
        LocalDateTime authorizedAt,
        LocalDateTime capturedAt) {}
