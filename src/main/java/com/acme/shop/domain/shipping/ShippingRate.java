package com.acme.shop.domain.shipping;

import com.acme.shop.domain.order.Money;

public record ShippingRate(String carrier, String country, Money baseCost, Money perItemCost) {

    public ShippingRate {
        if (carrier == null || carrier.isBlank()) {
            throw new IllegalArgumentException("Carrier must not be null or blank");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Country must not be null or blank");
        }
    }

    public Money calculateCost(int itemCount) {
        return baseCost.add(perItemCost.multiply(itemCount));
    }
}
