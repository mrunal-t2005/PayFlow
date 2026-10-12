
package com.payflow.controller;

import com.payflow.entity.Refund;
import com.payflow.service.RefundService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping("/payments/{paymentId}/refunds")
    public ResponseEntity<Refund> createRefund(
            @PathVariable UUID paymentId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CreateRefundRequest request) {

        Refund refund = refundService.createRefund(
                paymentId,
                request.amount(),
                idempotencyKey);

        return ResponseEntity
                .created(URI.create("/api/refunds/" + refund.getId()))
                .body(refund);
    }

    @GetMapping("/refunds/{refundId}")
    public Refund getRefund(@PathVariable UUID refundId) {
        return refundService.getRefund(refundId);
    }
    
    
    @PostMapping("/refunds/{refundId}/process")
    public Refund processRefund(@PathVariable UUID refundId) {
        return refundService.processRefund(refundId);
    }

    @GetMapping("/payments/{paymentId}/refunds")
    public List<Refund> getRefundsForPayment(
            @PathVariable UUID paymentId) {
        return refundService.getRefundsForPayment(paymentId);
    }

    public record CreateRefundRequest(BigDecimal amount) {
    }
}
