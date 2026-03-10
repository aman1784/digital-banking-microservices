package com.bank.transactionservice.controller;

import com.bank.transactionservice.dto.DepositRequest;
import com.bank.transactionservice.dto.TransactionResponse;
import com.bank.transactionservice.dto.WithdrawRequest;
import com.bank.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<String> deposit(
            @RequestHeader("X-User-Name") String username,
            @PathVariable Long accountId,
            @RequestBody @Valid DepositRequest request) {
        service.processDeposit(accountId, request.amount(), username);
        return ResponseEntity.ok("Transaction completed");
    }

    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<String> withdraw(
            @RequestHeader("X-User-Name") String username,
            @PathVariable Long accountId,
            @RequestBody @Valid WithdrawRequest request) {

        service.processWithdraw(accountId, request.amount(), username);
        return ResponseEntity.ok("Transaction completed");
    }

    @GetMapping("/me")
    public ResponseEntity<Page<TransactionResponse>> getMyTransactions(
            @RequestHeader("X-User-Name") String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(service.getUserTransactions(username, page, size));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<Page<TransactionResponse>> getAccountTransactions(
            @RequestHeader("X-User-Name") String username,
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(service.getAccountTransactions(accountId, username, page, size));
    }
}