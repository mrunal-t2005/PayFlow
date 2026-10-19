
package com.payflow;

import com.payflow.entity.Payment;
import com.payflow.entity.Refund;
import com.payflow.repository.PaymentRepository;
import com.payflow.repository.RefundRepository;
import com.payflow.service.LedgerService;
import com.payflow.service.RefundService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceTests {

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private LedgerService ledgerService;

    @InjectMocks
    private RefundService refundService;

    private UUID paymentId;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        payment = new Payment(
                new BigDecimal("250.00"),
                "INR",
                "payment-test-" + UUID.randomUUID()
        );

        payment.markProcessing();
        payment.markSucceeded();
    }

    @Test
    void createRefundShouldSavePendingRefundWhenAmountIsValid() {
        when(refundRepository.findByIdempotencyKey("refund-001"))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByIdForUpdate(paymentId))
                .thenReturn(Optional.of(payment));
        when(refundRepository.findByPaymentId(paymentId))
                .thenReturn(List.of());
        when(refundRepository.save(any(Refund.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Refund result = refundService.createRefund(
                paymentId,
                new BigDecimal("100.00"),
                "refund-001"
        );

        assertEquals(Refund.RefundStatus.PENDING, result.getStatus());
        assertEquals(0, result.getAmount().compareTo(new BigDecimal("100.00")));
        assertEquals(paymentId, result.getPaymentId());

        verify(paymentRepository).findByIdForUpdate(paymentId);
        verify(refundRepository).save(any(Refund.class));
    }

    @Test
    void createRefundShouldRejectAmountExceedingRemainingBalance() {
        Refund previousRefund = new Refund(
                paymentId,
                new BigDecimal("200.00"),
                "INR",
                "refund-previous"
        );

        when(refundRepository.findByIdempotencyKey("refund-new"))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByIdForUpdate(paymentId))
                .thenReturn(Optional.of(payment));
        when(refundRepository.findByPaymentId(paymentId))
                .thenReturn(List.of(previousRefund));

        assertThrows(
                IllegalArgumentException.class,
                () -> refundService.createRefund(
                        paymentId,
                        new BigDecimal("100.00"),
                        "refund-new"
                )
        );

        verify(refundRepository, never()).save(any(Refund.class));
    }

    @Test
    void createRefundShouldReturnExistingRefundForSameIdempotencyKey() {
        Refund existingRefund = new Refund(
                paymentId,
                new BigDecimal("100.00"),
                "INR",
                "refund-retry"
        );

        when(refundRepository.findByIdempotencyKey("refund-retry"))
                .thenReturn(Optional.of(existingRefund));

        Refund result = refundService.createRefund(
                paymentId,
                new BigDecimal("100.00"),
                "refund-retry"
        );

        assertSame(existingRefund, result);
        verify(paymentRepository, never()).findByIdForUpdate(any(UUID.class));
        verify(refundRepository, never()).save(any(Refund.class));
    }

    @Test
    void createRefundShouldRejectReusingKeyWithDifferentAmount() {
        Refund existingRefund = new Refund(
                paymentId,
                new BigDecimal("100.00"),
                "INR",
                "refund-reuse"
        );

        when(refundRepository.findByIdempotencyKey("refund-reuse"))
                .thenReturn(Optional.of(existingRefund));

        assertThrows(
                IllegalStateException.class,
                () -> refundService.createRefund(
                        paymentId,
                        new BigDecimal("150.00"),
                        "refund-reuse"
                )
        );

        verify(refundRepository, never()).save(any(Refund.class));
    }

    
@Test
void processRefundShouldRecordLedgerAndMarkRefundSucceeded()
        throws Exception {

    UUID refundId = UUID.randomUUID();

    Refund refund = new Refund(
            paymentId,
            new BigDecimal("100.00"),
            "INR",
            "refund-process"
    );

    // Simulate the UUID that JPA generates when saving the refund.
    var idField = Refund.class.getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(refund, refundId);

    when(refundRepository.findByIdForUpdate(refundId))
            .thenReturn(Optional.of(refund));
    when(paymentRepository.findById(paymentId))
            .thenReturn(Optional.of(payment));
    when(refundRepository.save(any(Refund.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    Refund result = refundService.processRefund(refundId);

    assertEquals(Refund.RefundStatus.SUCCEEDED, result.getStatus());

    verify(ledgerService).recordTransaction(
            eq(refundId),
            anyList()
    );

    verify(refundRepository).save(refund);
}


    @Test
    void processRefundShouldRejectAlreadyProcessedRefund() {
        UUID refundId = UUID.randomUUID();

        Refund refund = new Refund(
                paymentId,
                new BigDecimal("100.00"),
                "INR",
                "refund-processed"
        );
        refund.markSucceeded();

        when(refundRepository.findByIdForUpdate(refundId))
                .thenReturn(Optional.of(refund));

        assertThrows(
                IllegalStateException.class,
                () -> refundService.processRefund(refundId)
        );

        verify(ledgerService, never()).recordTransaction(
                any(UUID.class),
                anyList()
        );
    }
}
