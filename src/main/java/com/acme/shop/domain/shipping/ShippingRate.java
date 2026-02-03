package com.acme.shop.domain.shipping;

import com.acme.shop.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "shipping_rates")
public class ShippingRate extends BaseEntity {

    @Column(nullable = false)
    private String carrier;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal baseCost;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal perItemCost;

    private String currency;

    public ShippingRate() {}

    public String getCarrier() { return carrier; }
    public void setCarrier(String carrier) { this.carrier = carrier; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public BigDecimal getBaseCost() { return baseCost; }
    public void setBaseCost(BigDecimal baseCost) { this.baseCost = baseCost; }

    public BigDecimal getPerItemCost() { return perItemCost; }
    public void setPerItemCost(BigDecimal perItemCost) { this.perItemCost = perItemCost; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
