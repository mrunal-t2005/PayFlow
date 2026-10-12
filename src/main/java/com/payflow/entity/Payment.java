
package com.payflow.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

   @Column(nullable = false, unique = true, updatable = false, length = 100)
   private String idempotencyKey; 

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Payment() {
    }

    public Payment(BigDecimal amount, String currency, String idempotencyKey) {
    this.amount = amount;
    this.currency = currency;
    this.idempotencyKey = idempotencyKey;
    this.status = "PENDING";
    this.createdAt = Instant.now();
}
    public UUID getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getIdempotencyKey() {
    return idempotencyKey;
}
    public void markProcessing() {
        if (!"PENDING".equals(this.status)) {
            throw new IllegalStateException(
                    "Only PENDING payments can be processed."
            );
        }

        this.status = "PROCESSING";
    }

    public void markSucceeded() {
        if (!"PROCESSING".equals(this.status)) {
            throw new IllegalStateException(
                    "Only PROCESSING payments can succeed."
            );
        }

        this.status = "SUCCEEDED";
    }

    public void markFailed() {
        if (!"PROCESSING".equals(this.status)) {
            throw new IllegalStateException(
                    "Only PROCESSING payments can fail."
            );
        }

        this.status = "FAILED";
    }
}


