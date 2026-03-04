package com.acme.shop.infrastructure.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public final class MoneyUtils {

    private MoneyUtils() {}

    public static BigDecimal round(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static String format(BigDecimal amount, String currencyCode) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        formatter.setCurrency(Currency.getInstance(currencyCode));
        return formatter.format(amount);
    }

    public static BigDecimal applyDiscount(BigDecimal amount, BigDecimal discountPercent) {
        BigDecimal factor = BigDecimal.ONE.subtract(discountPercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        return round(amount.multiply(factor));
    }

    public static BigDecimal addTax(BigDecimal amount, BigDecimal taxRate) {
        BigDecimal tax = amount.multiply(taxRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        return round(amount.add(tax));
    }
}
