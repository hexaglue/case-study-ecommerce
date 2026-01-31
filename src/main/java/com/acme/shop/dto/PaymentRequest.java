package com.acme.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
        @NotNull Long orderId,
        @NotBlank String paymentMethod) {}
