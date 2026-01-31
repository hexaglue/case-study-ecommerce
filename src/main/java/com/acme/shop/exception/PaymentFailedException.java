package com.acme.shop.exception;

public class PaymentFailedException extends RuntimeException {

    private final String paymentReference;

    public PaymentFailedException(String paymentReference, String reason) {
        super("Payment failed for " + paymentReference + ": " + reason);
        this.paymentReference = paymentReference;
    }

    public String getPaymentReference() {
        return paymentReference;
    }
}
