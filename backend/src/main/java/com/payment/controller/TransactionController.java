package com.payment.controller;

import com.payment.dto.TransactionDTO;
import com.payment.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for viewing payment transaction history.
 * Transactions are stored locally in PostgreSQL for fast querying.
 *
 * Requirements: 9.1, 9.3, 9.5
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * GET /api/transactions — All transactions ordered by timestamp descending.
     * GET /api/transactions?customerId={id} — Filtered by customer.
     * Requirement: 9.1, 9.3, 9.5
     */
    @GetMapping
    public ResponseEntity<List<TransactionDTO>> getTransactions(
            @RequestParam(required = false) String customerId) {
        List<TransactionDTO> transactions;
        if (customerId != null && !customerId.isBlank()) {
            transactions = transactionService.getTransactionsByCustomer(customerId);
        } else {
            transactions = transactionService.getAllTransactions();
        }
        return ResponseEntity.ok(transactions);
    }
}
