
package com.payflow.service;

import com.payflow.entity.LedgerEntry;
import com.payflow.entity.Payment;
import com.payflow.repository.PaymentRepository;
import com.payflow.service.LedgerService;

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
            String currencyCode) {

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

        // Step 1: Save the payment.
        Payment payment =
                paymentRepository.save(
                        new Payment(amount, normalizedCurrency));

        // Step 2: Create matching debit and credit entries.
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

        // Step 3: Validate and record the balanced ledger entries.
        ledgerService.recordTransaction(
                payment.getId(), entries);

        return payment;
    }

    public Payment getPayment(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment not found: " + id));
    }
}

