package com.acme.shop.infrastructure.web.dto;

import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String street,
        String city,
        String zipCode,
        String country) {}
