
package com.payflow.service;

import com.payflow.entity.LedgerEntry;

import com.payflow.entity.Payment;
import com.payflow.entity.Refund;
import com.payflow.repository.PaymentRepository;
import com.payflow.repository.RefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class RefundService {
    private final LedgerService ledgerService;
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;

    public RefundService(
        RefundRepository refundRepository,
        PaymentRepository paymentRepository,
        LedgerService ledgerService) {
    this.refundRepository = refundRepository;
    this.paymentRepository = paymentRepository;
    this.ledgerService = ledgerService;
}

    @Transactional
    public Refund createRefund(
            UUID paymentId,
            BigDecimal amount,
            String idempotencyKey) {

        if (paymentId == null) {
            throw new IllegalArgumentException("Payment ID is required");
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency key is required");
        }

        idempotencyKey = idempotencyKey.trim();

        if (idempotencyKey.length() > 100) {
            throw new IllegalArgumentException(
                    "Idempotency key must not exceed 100 characters");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Refund amount must be greater than zero");
        }

        Refund existing = refundRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElse(null);

        if (existing != null) {
            boolean samePayment = existing.getPaymentId().equals(paymentId);
            boolean sameAmount = existing.getAmount().compareTo(amount) == 0;

            if (!samePayment || !sameAmount) {
                throw new IllegalStateException(
                        "Idempotency key already used with different refund details");
            }

            return existing;
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Payment not found: " + paymentId));

        if (!"SUCCEEDED".equals(payment.getStatus())) {
            throw new IllegalStateException(
                    "Only succeeded payments can be refunded");
        }

        List<Refund> refunds = refundRepository.findByPaymentId(paymentId);

        BigDecimal alreadyRefunded = refunds.stream()
                .filter(refund ->
                        refund.getStatus() != Refund.RefundStatus.FAILED)
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remaining = payment.getAmount().subtract(alreadyRefunded);

        if (amount.compareTo(remaining) > 0) {
            throw new IllegalArgumentException(
                    "Refund exceeds remaining refundable amount: " + remaining);
        }

        String currency = payment.getCurrency().toUpperCase(Locale.ROOT);

        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Unsupported payment currency: " + currency);
        }

        return refundRepository.save(
                new Refund(paymentId, amount, currency, idempotencyKey));
    }
    
    @Transactional
    public Refund processRefund(UUID refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Refund not found: " + refundId));

        if (refund.getStatus() != Refund.RefundStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending refunds can be processed");
        }

        Payment payment = paymentRepository.findById(refund.getPaymentId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Payment not found: " + refund.getPaymentId()));

        List<LedgerEntry> reversalEntries = List.of(
                new LedgerEntry(
                        refund.getId(),
                        refund.getAmount(),
                        LedgerEntry.EntryType.DEBIT,
                        payment.getCurrency()),
                new LedgerEntry(
                        refund.getId(),
                        refund.getAmount(),
                        LedgerEntry.EntryType.CREDIT,
                        payment.getCurrency())
        );

        ledgerService.recordTransaction(refund.getId(), reversalEntries);

        refund.markSucceeded();

        return refundRepository.save(refund);
    }

    @Transactional(readOnly = true)
    public Refund getRefund(UUID refundId) {
        return refundRepository.findById(refundId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Refund not found: " + refundId));
    }

    @Transactional(readOnly = true)
    public List<Refund> getRefundsForPayment(UUID paymentId) {
        if (!paymentRepository.existsById(paymentId)) {
            throw new IllegalArgumentException(
                    "Payment not found: " + paymentId);
        }

        return refundRepository.findByPaymentId(paymentId);
    }
}
