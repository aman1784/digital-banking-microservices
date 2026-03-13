package com.bank.transactionservice.executorService;

import com.bank.transactionservice.entity.Transaction;
import com.bank.transactionservice.enums.TransactionStatus;
import com.bank.transactionservice.exception.AccountServiceUnavailableException;
import com.bank.transactionservice.exception.InsufficientBalanceException;
import com.bank.transactionservice.exception.TransactionNotFoundException;
import com.bank.transactionservice.exception.UnauthorizedAccountAccessException;
import com.bank.transactionservice.feignclient.AccountClient;
import com.bank.transactionservice.kafka.TransactionEvent;
import com.bank.transactionservice.kafka.TransactionEventProducer;
import com.bank.transactionservice.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class DepositExecutorService {

    private final AccountClient accountClient;
    private final TransactionRepository repository;
    private final TransactionEventProducer producer;

    public DepositExecutorService(AccountClient accountClient, TransactionRepository repository, TransactionEventProducer producer) {
        this.accountClient = accountClient;
        this.repository = repository;
        this.producer = producer;
    }

    @CircuitBreaker(name = "accountServiceCB", fallbackMethod = "depositFallback")
    public void depositInternal(Long transactionId,
                                Long accountId,
                                BigDecimal amount,
                                String username){
        log.info("Calling account-service deposit accountId={} amount={}", accountId, amount);

        accountClient.deposit(accountId, amount);

        Transaction transaction = repository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));

        transaction.setStatus(TransactionStatus.SUCCESS);

        repository.save(transaction);

        log.info("Transaction {} marked SUCCESS", transactionId);

        publishTransactionEvent(transaction);
    }

    public void depositFallback(Long transactionId,
                                Long accountId,
                                BigDecimal amount,
                                String username,
                                Throwable ex){
        log.error("Deposit failed transactionId={} error={}", transactionId, ex.getMessage());

        Transaction transaction = repository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));

        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setFailureReason(ex.getMessage());

        repository.save(transaction);

        publishTransactionEvent(transaction);

        if (ex instanceof feign.FeignException.BadRequest) {
            throw new InsufficientBalanceException("Insufficient balance or account inactive");
        }

        if (ex instanceof feign.FeignException.Forbidden) {
            throw new UnauthorizedAccountAccessException("Account not found or inactive");
        }

        throw new AccountServiceUnavailableException(
                "Account service temporarily unavailable. Please try again later."
        );
    }

    private void publishTransactionEvent(Transaction saved) {

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

        log.info("Transaction event published for transactionId={}", saved.getId());
    }
}
