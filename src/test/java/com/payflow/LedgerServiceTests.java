package com.payflow;

import com.payflow.entity.LedgerEntry;
import com.payflow.entity.LedgerEntry.EntryType;
import com.payflow.repository.LedgerEntryRepository;
import com.payflow.service.LedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LedgerServiceTests {

    private LedgerEntryRepository repository;
    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        repository = mock(LedgerEntryRepository.class);
        ledgerService = new LedgerService(repository);
    }

    @Test
    void shouldAcceptBalancedTransaction() {
        UUID transactionId = UUID.randomUUID();

        List<LedgerEntry> entries = List.of(
                new LedgerEntry(
                        transactionId,
                        new BigDecimal("499.00"),
                        EntryType.DEBIT,
                        "INR"),
                new LedgerEntry(
                        transactionId,
                        new BigDecimal("499.00"),
                        EntryType.CREDIT,
                        "INR")
        );

        when(repository.saveAll(anyList())).thenReturn(entries);

        List<LedgerEntry> result =
                ledgerService.recordTransaction(transactionId, entries);

        assertEquals(2, result.size());
        verify(repository).saveAll(entries);
    }

    @Test
    void shouldRejectUnbalancedTransaction() {
        UUID transactionId = UUID.randomUUID();

        List<LedgerEntry> entries = List.of(
                new LedgerEntry(
                        transactionId,
                        new BigDecimal("499.00"),
                        EntryType.DEBIT,
                        "INR"),
                new LedgerEntry(
                        transactionId,
                        new BigDecimal("400.00"),
                        EntryType.CREDIT,
                        "INR")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ledgerService.recordTransaction(
                        transactionId, entries)
        );
    }

    @Test
    void shouldRejectNonPositiveAmount() {
        UUID transactionId = UUID.randomUUID();

        List<LedgerEntry> entries = List.of(
                new LedgerEntry(
                        transactionId,
                        new BigDecimal("0.00"),
                        EntryType.DEBIT,
                        "INR"),
                new LedgerEntry(
                        transactionId,
                        new BigDecimal("0.00"),
                        EntryType.CREDIT,
                        "INR")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ledgerService.recordTransaction(
                        transactionId, entries)
        );
    }
}
