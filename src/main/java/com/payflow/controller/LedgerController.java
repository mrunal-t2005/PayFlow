
package com.payflow.controller;

import com.payflow.entity.LedgerEntry;
import com.payflow.service.LedgerService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/{transactionId}")
    public List<LedgerEntry> getLedgerEntries(
            @PathVariable UUID transactionId) {

        return ledgerService.getEntries(transactionId);
    }
}

