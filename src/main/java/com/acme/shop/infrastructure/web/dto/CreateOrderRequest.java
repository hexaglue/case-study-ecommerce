package com.acme.shop.infrastructure.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull UUID customerId,
        @NotEmpty List<LineItem> items,
        String shippingStreet,
        String shippingCity,
        String shippingZipCode,
        String shippingCountry) {

    public record LineItem(
            @NotNull UUID productId,
            @Positive int quantity) {}
}
