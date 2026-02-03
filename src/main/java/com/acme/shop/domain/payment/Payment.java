package com.acme.shop.domain.payment;

import com.acme.shop.domain.order.Money;
import com.acme.shop.domain.order.OrderId;
import java.time.LocalDateTime;

public class Payment {

    private PaymentId id;
    private final String paymentReference;
    private final OrderId orderId;
    private final Money amount;
    private final String paymentMethod;
    private PaymentStatus status;
    private String transactionId;
    private LocalDateTime authorizedAt;
    private LocalDateTime capturedAt;
    private LocalDateTime failedAt;
    private String failureReason;

    public Payment(PaymentId id, String paymentReference, OrderId orderId, Money amount,
                   String paymentMethod, PaymentStatus status, String transactionId,
                   LocalDateTime authorizedAt, LocalDateTime capturedAt,
                   LocalDateTime failedAt, String failureReason) {
        this.id = id;
        this.paymentReference = paymentReference;
        this.orderId = orderId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.transactionId = transactionId;
        this.authorizedAt = authorizedAt;
        this.capturedAt = capturedAt;
        this.failedAt = failedAt;
        this.failureReason = failureReason;
    }

    public static Payment create(String paymentReference, OrderId orderId, Money amount, String paymentMethod) {
        return new Payment(null, paymentReference, orderId, amount, paymentMethod,
                PaymentStatus.PENDING, null, null, null, null, null);
    }

    public void authorize(String transactionId) {
        this.status = PaymentStatus.AUTHORIZED;
        this.transactionId = transactionId;
        this.authorizedAt = LocalDateTime.now();
    }

    public void capture() {
        if (status != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Payment must be AUTHORIZED to capture");
        }
        this.status = PaymentStatus.CAPTURED;
        this.capturedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failedAt = LocalDateTime.now();
        this.failureReason = reason;
    }

    public PaymentId getId() { return id; }
    public void setId(PaymentId id) { this.id = id; }
    public String getPaymentReference() { return paymentReference; }
    public OrderId getOrderId() { return orderId; }
    public Money getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public String getTransactionId() { return transactionId; }
    public LocalDateTime getAuthorizedAt() { return authorizedAt; }
    public LocalDateTime getCapturedAt() { return capturedAt; }
    public LocalDateTime getFailedAt() { return failedAt; }
    public String getFailureReason() { return failureReason; }
}
