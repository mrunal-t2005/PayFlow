
package com.payflow;

import com.payflow.entity.Payment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PaymentStateTests {

    private Payment createPayment() {
        return new Payment(
                new BigDecimal("250.00"),
                "INR"
        );
    }

    @Test
    void newPaymentStartsAsPending() {
        Payment payment = createPayment();

        assertEquals("PENDING", payment.getStatus());
    }

    @Test
    void pendingPaymentCanBecomeProcessing() {
        Payment payment = createPayment();

        payment.markProcessing();

        assertEquals("PROCESSING", payment.getStatus());
    }

    @Test
    void processingPaymentCanSucceed() {
        Payment payment = createPayment();

        payment.markProcessing();
        payment.markSucceeded();

        assertEquals("SUCCEEDED", payment.getStatus());
    }

    @Test
    void processingPaymentCanFail() {
        Payment payment = createPayment();

        payment.markProcessing();
        payment.markFailed();

        assertEquals("FAILED", payment.getStatus());
    }

    @Test
    void succeededPaymentCannotBeProcessedAgain() {
        Payment payment = createPayment();

        payment.markProcessing();
        payment.markSucceeded();

        assertThrows(
                IllegalStateException.class,
                payment::markProcessing
        );

        assertEquals("SUCCEEDED", payment.getStatus());
    }

    @Test
    void pendingPaymentCannotSucceedDirectly() {
        Payment payment = createPayment();

        assertThrows(
                IllegalStateException.class,
                payment::markSucceeded
        );

        assertEquals("PENDING", payment.getStatus());
    }
}

