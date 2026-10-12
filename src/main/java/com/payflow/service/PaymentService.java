

package com.payflow.service;

import com.payflow.entity.LedgerEntry;
import com.payflow.entity.Payment;
import com.payflow.repository.PaymentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final LedgerService ledgerService;

    public PaymentService(
            PaymentRepository paymentRepository,
            LedgerService ledgerService) {
        this.paymentRepository = paymentRepository;
        this.ledgerService = ledgerService;
    }

    
@Transactional
public Payment createPayment(
        BigDecimal amount,
        String currencyCode,
        String idempotencyKey) {

    if (idempotencyKey == null || idempotencyKey.isBlank()) {
        throw new IllegalArgumentException(
                "Idempotency key is required");
    }

    idempotencyKey = idempotencyKey.trim();

    if (idempotencyKey.length() > 100) {
        throw new IllegalArgumentException(
                "Idempotency key must not exceed 100 characters");
    }

    if (amount == null ||
            amount.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException(
                "Payment amount must be greater than zero");
    }

    if (currencyCode == null ||
            !currencyCode.matches("[A-Za-z]{3}")) {
        throw new IllegalArgumentException(
                "Currency must be a valid three-letter currency code");
    }

    String normalizedCurrency =
            currencyCode.toUpperCase(Locale.ROOT);

    try {
        Currency.getInstance(normalizedCurrency);
    } catch (IllegalArgumentException ex) {
        throw new IllegalArgumentException(
                "Unsupported currency: " + normalizedCurrency);
    }

    var existingPayment =
            paymentRepository.findByIdempotencyKey(idempotencyKey);

    if (existingPayment.isPresent()) {
        Payment payment = existingPayment.get();

        boolean sameAmount =
                payment.getAmount().compareTo(amount) == 0;

        boolean sameCurrency =
                payment.getCurrency().equals(normalizedCurrency);

        if (!sameAmount || !sameCurrency) {
            throw new IllegalStateException(
                    "Idempotency key has already been used with different payment details");
        }

        return payment;
    }

    Payment payment = paymentRepository.save(
            new Payment(amount, normalizedCurrency, idempotencyKey));

    List<LedgerEntry> entries = List.of(
            new LedgerEntry(
                    payment.getId(),
                    amount,
                    LedgerEntry.EntryType.DEBIT,
                    normalizedCurrency),
            new LedgerEntry(
                    payment.getId(),
                    amount,
                    LedgerEntry.EntryType.CREDIT,
                    normalizedCurrency)
    );

    ledgerService.recordTransaction(payment.getId(), entries);

    return payment;
}

    @Transactional
    public Payment processPayment(UUID id) {
        Payment payment = getPayment(id);

        payment.markProcessing();

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment succeedPayment(UUID id) {
        Payment payment = getPayment(id);

        payment.markSucceeded();

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment failPayment(UUID id) {
        Payment payment = getPayment(id);

        payment.markFailed();

        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Payment getPayment(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment not found: " + id));
    }
}
