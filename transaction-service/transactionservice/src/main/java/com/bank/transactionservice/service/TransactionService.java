package com.bank.transactionservice.service;

import com.bank.transactionservice.dto.TransactionResponse;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public interface TransactionService {

    void processDeposit(Long accountId, BigDecimal amount, String username);

    void processWithdraw(Long accountId, BigDecimal amount, String username);

    Page<TransactionResponse> getUserTransactions(String username, int page, int size);

    Page<TransactionResponse> getAccountTransactions(Long accountId, String username, int page, int size);
}