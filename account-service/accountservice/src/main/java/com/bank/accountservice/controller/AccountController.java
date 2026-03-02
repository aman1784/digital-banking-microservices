package com.bank.accountservice.controller;

import com.bank.accountservice.dto.BalanceResponse;
import com.bank.accountservice.dto.CreateAccountRequest;
import com.bank.accountservice.entity.Account;
import com.bank.accountservice.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestHeader("X-User-Name") String username,
                                                 @Valid @RequestBody CreateAccountRequest request) {

        return ResponseEntity.ok(accountService.createAccount(request, username));
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<String> deposit(@RequestHeader("X-User-Name") String username, @PathVariable Long id, @RequestParam BigDecimal amount) {
        accountService.deposit(id, amount, username);
        return ResponseEntity.ok("Deposit successful");
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<String> withdraw(@RequestHeader("X-User-Name") String username, @PathVariable Long id, @RequestParam BigDecimal amount) {
        accountService.withdraw(id, amount, username);
        return ResponseEntity.ok("Withdraw successful");
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@RequestHeader("X-User-Name") String username, @PathVariable Long id) {
        return ResponseEntity.ok(accountService.getBalance(id, username));
    }

    @PutMapping("/admin/accounts/{id}/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> freezeAccount(@PathVariable Long id) {
        accountService.freezeAccount(id);
        return ResponseEntity.ok("Account frozen successfully");
    }

    @PutMapping("/admin/accounts/{id}/unfreeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> unfreezeAccount(@PathVariable Long id) {
         accountService.unfreezeAccount(id);
        return ResponseEntity.ok("Account unfrozen successfully");
    }
}
