
package com.payflow.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refunds")
public class Refund {

    public enum RefundStatus {
        PENDING,
        SUCCEEDED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID paymentId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus status;

    @Column(nullable = false, unique = true, updatable = false, length = 100)
    private String idempotencyKey;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Refund() {
    }

    public Refund(
            UUID paymentId,
            BigDecimal amount,
            String currency,
            String idempotencyKey) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
        this.status = RefundStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void markSucceeded() {
        if (this.status != RefundStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending refunds can succeed.");
        }
        this.status = RefundStatus.SUCCEEDED;
    }

    public void markFailed() {
        if (this.status != RefundStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending refunds can fail.");
        }
        this.status = RefundStatus.FAILED;
    }
}
