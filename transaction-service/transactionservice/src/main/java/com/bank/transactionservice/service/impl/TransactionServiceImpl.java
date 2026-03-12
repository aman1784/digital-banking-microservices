package com.bank.transactionservice.service.impl;

import com.bank.transactionservice.dto.TransactionResponse;
import com.bank.transactionservice.entity.Transaction;
import com.bank.transactionservice.enums.TransactionStatus;
import com.bank.transactionservice.enums.TransactionType;
import com.bank.transactionservice.exception.AccountServiceUnavailableException;
import com.bank.transactionservice.exception.InsufficientBalanceException;
import com.bank.transactionservice.exception.UnauthorizedAccountAccessException;
import com.bank.transactionservice.executorService.DepositExecutorService;
import com.bank.transactionservice.executorService.WithdrawExecutorService;
import com.bank.transactionservice.feignclient.AccountClient;
import com.bank.transactionservice.kafka.TransactionEvent;
import com.bank.transactionservice.kafka.TransactionEventProducer;
import com.bank.transactionservice.repository.TransactionRepository;
import com.bank.transactionservice.service.TransactionService;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository repository;
    private final AccountClient accountClient;
    private final TransactionEventProducer producer;
    private final WithdrawExecutorService withdrawExecutorService;
    private final DepositExecutorService depositExecutorService;

    public TransactionServiceImpl(TransactionRepository repository,
                                  AccountClient accountClient,
                                  TransactionEventProducer producer,
                                  WithdrawExecutorService withdrawExecutorService,
                                  DepositExecutorService depositExecutorService) {
        this.repository = repository;
        this.accountClient = accountClient;
        this.producer = producer;
        this.withdrawExecutorService = withdrawExecutorService;
        this.depositExecutorService = depositExecutorService;
    }

    @Override
    public void processDeposit(Long accountId, BigDecimal amount, String username) {

        // 1. Save as PENDING
        Transaction transaction = repository.save(
                Transaction.builder()
                        .accountId(accountId)
                        .username(username)
                        .type(TransactionType.DEPOSIT)
                        .amount(amount)
                        .status(TransactionStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .build());

        log.info("[TransactionServiceImpl][processDeposit] Pending Transaction with id {} has been created", transaction.getId());

        depositExecutorService.depositInternal(
                transaction.getId(),
                accountId,
                amount,
                username
        );
    }

    @Override
    public void processWithdraw(Long accountId, BigDecimal amount, String username) {

        // 1. Save as PENDING
        Transaction transaction = repository.save(
                Transaction.builder()
                        .accountId(accountId)
                        .username(username)
                        .type(TransactionType.WITHDRAW)
                        .amount(amount)
                        .status(TransactionStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        log.info("[TransactionServiceImpl][processWithdraw] Pending Transaction with id {} has been created", transaction.getId());

        // 2️. Call circuit breaker protected method
        withdrawExecutorService.withdrawInternal(
                transaction.getId(),
                accountId,
                amount,
                username
        );

    }

    @Override
    public Page<TransactionResponse> getUserTransactions(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.findByUsername(username, pageable).map(this::mapToResponse);
    }

    @Override
    public Page<TransactionResponse> getAccountTransactions(Long accountId, String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.findByAccountIdAndUsername(accountId, username, pageable).map(this::mapToResponse);
    }

    private TransactionResponse mapToResponse(Transaction tx) {

        return new TransactionResponse(
                tx.getId(),
                tx.getAccountId(),
                tx.getType().name(),
                tx.getAmount(),
                tx.getStatus().name(),
                tx.getFailureReason(),
                tx.getCreatedAt()
        );
    }





}
