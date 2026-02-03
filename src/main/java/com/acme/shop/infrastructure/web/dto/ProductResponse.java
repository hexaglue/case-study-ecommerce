package com.acme.shop.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String currency,
        String sku,
        String category,
        boolean active) {}
