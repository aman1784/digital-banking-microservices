package com.bank.transactionservice.service.impl;

import com.bank.transactionservice.dto.TransactionResponse;
import com.bank.transactionservice.entity.Transaction;
import com.bank.transactionservice.enums.TransactionStatus;
import com.bank.transactionservice.enums.TransactionType;
import com.bank.transactionservice.exception.AccountServiceUnavailableException;
import com.bank.transactionservice.exception.InsufficientBalanceException;
import com.bank.transactionservice.exception.UnauthorizedAccountAccessException;
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

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository repository;
    private final AccountClient accountClient;
    private final TransactionEventProducer producer;

    public TransactionServiceImpl(TransactionRepository repository,
                                  AccountClient accountClient, TransactionEventProducer producer) {
        this.repository = repository;
        this.accountClient = accountClient;
        this.producer = producer;
    }

    @Override
    @CircuitBreaker(name = "accountServiceCB", fallbackMethod = "depositFallback")
    public void processDeposit(Long accountId, BigDecimal amount, String username) {

        accountClient.deposit(accountId, amount);

        // Before Kafka
//        repository.save(Transaction.builder()
//                .accountId(accountId)
//                .username(username)
//                .type(TransactionType.DEPOSIT)
//                .amount(amount)
//                .status(TransactionStatus.SUCCESS)
//                .createdAt(LocalDateTime.now())
//                .build());

        // After Kafka
        Transaction saved = repository.save(Transaction.builder()
                .accountId(accountId)
                .username(username)
                .type(TransactionType.DEPOSIT)
                .amount(amount)
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build());

        producer.publish(
                new TransactionEvent(
                        saved.getId(),
                        saved.getAccountId(),
                        saved.getUsername(),
                        saved.getType().name(),
                        saved.getStatus().name(),
                        saved.getAmount(),
                        saved.getCreatedAt()
                )
        );
    }

    @Override
    @CircuitBreaker(name = "accountServiceCB", fallbackMethod = "withdrawFallback")
    public void processWithdraw(Long accountId, BigDecimal amount, String username) {

            accountClient.withdraw(accountId, amount);

        // Before Kafka
//            repository.save(Transaction.builder()
//                    .accountId(accountId)
//                    .username(username)
//                    .type(TransactionType.WITHDRAW)
//                    .amount(amount)
//                    .status(TransactionStatus.SUCCESS)
//                    .createdAt(LocalDateTime.now())
//                    .build());

        // After Kafka
        Transaction saved = repository.save(Transaction.builder()
                .accountId(accountId)
                .username(username)
                .type(TransactionType.WITHDRAW)
                .amount(amount)
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build());

        producer.publish(
                new TransactionEvent(
                        saved.getId(),
                        saved.getAccountId(),
                        saved.getUsername(),
                        saved.getType().name(),
                        saved.getStatus().name(),
                        saved.getAmount(),
                        saved.getCreatedAt()
                )
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


    public void depositFallback(Long accountId, BigDecimal amount, String username, Throwable ex) {

//        repository.save(Transaction.builder()
//                .accountId(accountId)
//                .username(username)
//                .type(TransactionType.DEPOSIT)
//                .amount(amount)
//                .status(TransactionStatus.FAILED)
//                .failureReason(ex.getMessage())
//                .createdAt(LocalDateTime.now())
//                .build());

        Transaction saved = repository.save(Transaction.builder()
                .accountId(accountId)
                .username(username)
                .type(TransactionType.DEPOSIT)
                .amount(amount)
                .status(TransactionStatus.FAILED)
                .failureReason(ex.getMessage())
                .createdAt(LocalDateTime.now())
                .build());

        producer.publish(
                new TransactionEvent(
                        saved.getId(),
                        saved.getAccountId(),
                        saved.getUsername(),
                        saved.getType().name(),
                        saved.getStatus().name(),
                        saved.getAmount(),
                        saved.getCreatedAt()
                )
        );

        if (ex.getMessage() != null && ex.getMessage().contains("400")){
            throw new InsufficientBalanceException("Insufficient balance or account inactive");
        }
        if (ex.getMessage() != null && ex.getMessage().contains("403")){
            throw new UnauthorizedAccountAccessException("Account not found or inactive");
        }
        throw new AccountServiceUnavailableException("Account service temporarily unavailable. Please try again later.");
    }

    public void withdrawFallback(Long accountId, BigDecimal amount, String username, Throwable ex) {

//        repository.save(Transaction.builder()
//                .accountId(accountId)
//                .username(username)
//                .type(TransactionType.WITHDRAW)
//                .amount(amount)
//                .status(TransactionStatus.FAILED)
//                .failureReason(ex.getMessage())
//                .createdAt(LocalDateTime.now())
//                .build());

        Transaction saved = repository.save(Transaction.builder()
                .accountId(accountId)
                .username(username)
                .type(TransactionType.WITHDRAW)
                .amount(amount)
                .status(TransactionStatus.FAILED)
                .failureReason(ex.getMessage())
                .createdAt(LocalDateTime.now())
                .build());

        producer.publish(
                new TransactionEvent(
                        saved.getId(),
                        saved.getAccountId(),
                        saved.getUsername(),
                        saved.getType().name(),
                        saved.getStatus().name(),
                        saved.getAmount(),
                        saved.getCreatedAt()
                )
        );

        if (ex.getMessage() != null && ex.getMessage().contains("400")){
            throw new InsufficientBalanceException("Insufficient balance or account inactive");
        }
        if (ex.getMessage() != null && ex.getMessage().contains("403")){
            throw new UnauthorizedAccountAccessException("Account not found or inactive");
        }
        throw new AccountServiceUnavailableException("Account service temporarily unavailable. Please try again later.");
    }
}
