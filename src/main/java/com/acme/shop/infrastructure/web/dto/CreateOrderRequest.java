package com.acme.shop.infrastructure.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record CreateOrderRequest(
        @NotNull Long customerId,
        @NotEmpty List<LineItem> items,
        String shippingStreet,
        String shippingCity,
        String shippingZipCode,
        String shippingCountry) {

    public record LineItem(
            @NotNull Long productId,
            @Positive int quantity) {}
}
