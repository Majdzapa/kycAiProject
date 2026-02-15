package com.kyc.ai.controller;

import com.kyc.ai.entity.FinancialTransaction;
import com.kyc.ai.repository.FinancialTransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Monitoring", description = "APIs for viewing and logging user transactions")
public class FinancialTransactionController {

    private final FinancialTransactionRepository transactionRepository;

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Get transactions for a customer")
    public List<FinancialTransaction> getCustomerTransactions(@PathVariable String customerId) {
        return transactionRepository.findByCustomerId(customerId);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // In real app, this would be from a core banking system
    @Operation(summary = "Log a new transaction (Simulation)")
    public FinancialTransaction logTransaction(@RequestBody FinancialTransaction transaction) {
        if (transaction.getTimestamp() == null) {
            transaction.setTimestamp(LocalDateTime.now());
        }
        return transactionRepository.save(transaction);
    }
}
