package com.acme.shop.domain.order;

public record Address(String street, String city, String zipCode, String country) {

    public Address {
        if (street == null || street.isBlank()) {
            throw new IllegalArgumentException("Street must not be null or blank");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City must not be null or blank");
        }
        if (zipCode == null || zipCode.isBlank()) {
            throw new IllegalArgumentException("Zip code must not be null or blank");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Country must not be null or blank");
        }
    }
}
