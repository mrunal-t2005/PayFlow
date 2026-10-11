package com.payflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {


public enum EntryType {
    DEBIT,
    CREDIT
}

@Id
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;

@Column(nullable = false)
private UUID transactionId;

@Column(nullable = false, precision = 19, scale = 2)
private BigDecimal amount;

@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 10)
private EntryType entryType;

@Column(nullable = false, length = 3)
private String currency;

@Column(nullable = false, updatable = false)
private Instant createdAt;

protected LedgerEntry() {
}

public LedgerEntry(
        UUID transactionId,
        BigDecimal amount,
        EntryType entryType,
        String currency) {
    this.transactionId = transactionId;
    this.amount = amount;
    this.entryType = entryType;
    this.currency = currency;
    this.createdAt = Instant.now();
}

public UUID getId() {
    return id;
}

public UUID getTransactionId() {
    return transactionId;
}

public BigDecimal getAmount() {
    return amount;
}

public EntryType getEntryType() {
    return entryType;
}

public String getCurrency() {
    return currency;
}

public Instant getCreatedAt() {
    return createdAt;
}


}
