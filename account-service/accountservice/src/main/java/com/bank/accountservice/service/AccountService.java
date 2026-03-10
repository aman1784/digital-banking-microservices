package com.bank.accountservice.service;

import com.bank.accountservice.dto.AccountResponse;
import com.bank.accountservice.dto.BalanceResponse;
import com.bank.accountservice.dto.CreateAccountRequest;
import com.bank.accountservice.entity.Account;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    Account createAccount(CreateAccountRequest request, String username);
    void deposit(Long accountId, BigDecimal amount, String username);
    void withdraw(Long accountId, BigDecimal amount, String username);
    BalanceResponse getBalance(Long accountId, String username);
    void freezeAccount(Long id);
    void unfreezeAccount(Long id);
    List<AccountResponse> getAllAccountsForUser(String username);
}
