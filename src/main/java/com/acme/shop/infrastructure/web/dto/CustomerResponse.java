package com.acme.shop.infrastructure.web.dto;

public record CustomerResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String street,
        String city,
        String zipCode,
        String country) {}
