
package com.payflow.controller;

import com.payflow.entity.Payment;
import com.payflow.service.PaymentService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Payment> createPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CreatePaymentRequest request) {

        Payment payment = paymentService.createPayment(
                request.amount(),
                request.currency(),
                idempotencyKey
        );

        return ResponseEntity
                .created(URI.create("/api/payments/" + payment.getId()))
                .body(payment);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPayment(
            @PathVariable UUID id) {

        return ResponseEntity.ok(paymentService.getPayment(id));
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<Payment> processPayment(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                paymentService.processPayment(id));
    }

    @PostMapping("/{id}/succeed")
    public ResponseEntity<Payment> succeedPayment(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                paymentService.succeedPayment(id));
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<Payment> failPayment(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                paymentService.failPayment(id));
    }

    public record CreatePaymentRequest(
            BigDecimal amount,
            String currency
    ) {}
}


