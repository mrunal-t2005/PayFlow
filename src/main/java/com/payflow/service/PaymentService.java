
package com.payflow.service;

import com.payflow.entity.Payment;
import com.payflow.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Payment createPayment(BigDecimal amount, String currencyCode) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                "Payment amount must be greater than zero"
            );
        }

        if (currencyCode == null ||
                !currencyCode.matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException(
                "Currency must be a valid three-letter currency code"
            );
        }

        String normalizedCurrency = currencyCode.toUpperCase(Locale.ROOT);

        try {
            Currency.getInstance(normalizedCurrency);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                "Unsupported currency: " + normalizedCurrency
            );
        }

        Payment payment = new Payment(amount, normalizedCurrency);

        return paymentRepository.save(payment);
    }

    public Payment getPayment(UUID id) {
        return paymentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(
                "Payment not found: " + id
            ));
    }
}