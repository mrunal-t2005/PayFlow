package com.payflow.service;

import com.payflow.entity.LedgerEntry;
import com.payflow.entity.LedgerEntry.EntryType;
import com.payflow.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public List<LedgerEntry> recordTransaction(
            UUID transactionId,
            List<LedgerEntry> entries) {

        if (transactionId == null) {
            throw new IllegalArgumentException(
                    "Transaction ID must not be null");
        }

        if (entries == null || entries.size() < 2) {
            throw new IllegalArgumentException(
                    "A transaction must contain at least two entries");
        }

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        Set<String> currencies = new HashSet<>();

        for (LedgerEntry entry : entries) {
            if (entry == null) {
                throw new IllegalArgumentException(
                        "Ledger entries must not be null");
            }

            if (!transactionId.equals(entry.getTransactionId())) {
                throw new IllegalArgumentException(
                        "All entries must use the supplied transaction ID");
            }

            if (entry.getAmount() == null
                    || entry.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Ledger amounts must be positive");
            }

            if (entry.getEntryType() == null) {
                throw new IllegalArgumentException(
                        "Entry type must be DEBIT or CREDIT");
            }

            if (entry.getCurrency() == null
                    || !entry.getCurrency().matches("[A-Z]{3}")) {
                throw new IllegalArgumentException(
                        "Currency must be a three-letter uppercase code");
            }

            currencies.add(entry.getCurrency());

            if (entry.getEntryType() == EntryType.DEBIT) {
                totalDebits = totalDebits.add(entry.getAmount());
            } else {
                totalCredits = totalCredits.add(entry.getAmount());
            }
        }

        if (currencies.size() != 1) {
            throw new IllegalArgumentException(
                    "All entries must use the same currency");
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new IllegalArgumentException(
                    "Transaction is unbalanced: debits must equal credits");
        }

        return ledgerEntryRepository.saveAll(entries);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> getEntries(UUID transactionId) {
        return ledgerEntryRepository.findByTransactionId(transactionId);
    }
}
