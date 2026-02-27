package com.bank.accountservice.service.impl;

import com.bank.accountservice.dto.BalanceResponse;
import com.bank.accountservice.dto.CreateAccountRequest;
import com.bank.accountservice.entity.Account;
import com.bank.accountservice.enums.AccountStatus;
import com.bank.accountservice.enums.AccountType;
import com.bank.accountservice.exception.InsufficientBalanceException;
import com.bank.accountservice.exception.UnauthorizedAccountAccessException;
import com.bank.accountservice.repository.AccountRepository;
import com.bank.accountservice.service.AccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public Account createAccount(CreateAccountRequest request, String username) {
        Account account = Account.builder()
                .accountNumber(UUID.randomUUID().toString())
                .ownerUsername(username)
                .accountType(AccountType.valueOf(request.accountType().toUpperCase()))
                .balance(request.initialDeposit())
                .status(AccountStatus.ACTIVE)
                .build();

        return accountRepository.save(account);
    }

    @Override
    @Transactional
    public void deposit(Long accountId, BigDecimal amount, String username) {

        int updated = accountRepository.deposit(accountId, amount, username);

        if (updated == 0) {
            throw new UnauthorizedAccountAccessException("Account not found or inactive");
        }
    }

    @Override
    @Transactional
    public void withdraw(Long accountId, BigDecimal amount, String username) {
        int updated = accountRepository.withdrawIfSufficient(accountId, amount, username);

        if (updated == 0) {
            throw new InsufficientBalanceException("Insufficient balance or account inactive");
        }
    }

    @Override
    public BalanceResponse getBalance(Long accountId, String username) {
        Account account = accountRepository
                .findByIdAndOwnerUsername(accountId, username)
                .orElseThrow(() ->
                        new UnauthorizedAccountAccessException(
                                "Account not found or access denied"
                        ));

        return new BalanceResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType().name(),
                account.getBalance(),
                account.getStatus().name()
        );
    }

    @Override
    public void freezeAccount(Long id) {
        return;
    }


}
