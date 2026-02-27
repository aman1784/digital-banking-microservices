package com.bank.transactionservice.service;

import com.bank.transactionservice.dto.TransactionResponse;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionService {

    void processDeposit(Long accountId, BigDecimal amount, String username);

    void processWithdraw(Long accountId, BigDecimal amount, String username);

    List<TransactionResponse> getUserTransactions(String username);

    List<TransactionResponse> getAccountTransactions(Long accountId, String username);
}
