package com.acme.shop.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PaymentRequest(
        @NotNull UUID orderId,
        @NotBlank String paymentMethod) {}
