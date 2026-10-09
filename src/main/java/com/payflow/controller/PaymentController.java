
package com.payflow.controller;

import com.payflow.entity.Payment;
import com.payflow.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;
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
            @RequestBody CreatePaymentRequest request) {

        Payment payment = paymentService.createPayment(
            request.amount(),
            request.currency()
        );

        return ResponseEntity
            .created(URI.create("/api/payments/" + payment.getId()))
            .body(payment);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getPayment(id));
    }

    public record CreatePaymentRequest(
        BigDecimal amount,
        String currency
    ) {}
}