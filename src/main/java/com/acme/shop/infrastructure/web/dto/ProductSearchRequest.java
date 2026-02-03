package com.acme.shop.infrastructure.web.dto;

import com.acme.shop.domain.product.Category;

public record ProductSearchRequest(
        String name,
        Category category,
        Boolean activeOnly) {}
