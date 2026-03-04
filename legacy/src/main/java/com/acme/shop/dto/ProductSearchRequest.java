package com.acme.shop.dto;

import com.acme.shop.model.Category;

public record ProductSearchRequest(
        String name,
        Category category,
        Boolean activeOnly) {}
