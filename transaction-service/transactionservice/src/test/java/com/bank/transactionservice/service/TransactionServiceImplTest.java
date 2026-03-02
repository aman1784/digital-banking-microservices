package com.bank.transactionservice.service;

import com.bank.transactionservice.dto.TransactionResponse;
import com.bank.transactionservice.entity.Transaction;
import com.bank.transactionservice.enums.TransactionStatus;
import com.bank.transactionservice.enums.TransactionType;
import com.bank.transactionservice.exception.InsufficientBalanceException;
import com.bank.transactionservice.exception.UnauthorizedAccountAccessException;
import com.bank.transactionservice.feignclient.AccountClient;
import com.bank.transactionservice.kafka.TransactionEvent;
import com.bank.transactionservice.kafka.TransactionEventProducer;
import com.bank.transactionservice.repository.TransactionRepository;
import com.bank.transactionservice.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DataJpaTest // Loads ONLY the JPA/H2 database slice, extremely fast!
@ActiveProfiles("test") // Uses application-test.yml
@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Autowired
    private TransactionRepository transactionRepository; // Real H2-backed repository

    @Mock
    private AccountClient accountClient; // Mocked Feign Client

    @Mock
    private TransactionEventProducer producer; // Mocked Kafka Producer

    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setUp() {
        // Clear DB before each test
        transactionRepository.deleteAll();
        // Inject the real repo and mocked services into our Service implementation
        transactionService = new TransactionServiceImpl(transactionRepository, accountClient, producer);
    }

    @Test
    void processDeposit_Success() {
        // Arrange
        Long accountId = 1L;
        BigDecimal amount = new BigDecimal("500.00");
        String username = "aman";

        // Mock Feign call to do nothing (simulate success)
        doNothing().when(accountClient).deposit(accountId, amount);

        // Act
        transactionService.processDeposit(accountId, amount, username);

        // Assert 1: Verify DB State
        List<Transaction> savedTransactions = transactionRepository.findAll();
        assertEquals(1, savedTransactions.size());

        Transaction savedTx = savedTransactions.get(0);
        assertEquals(TransactionType.DEPOSIT, savedTx.getType());
        assertEquals(TransactionStatus.SUCCESS, savedTx.getStatus());
        assertEquals(amount, savedTx.getAmount());
        assertEquals(username, savedTx.getUsername());
        assertNull(savedTx.getFailureReason());

        // Assert 2: Verify Kafka Event was published
        ArgumentCaptor<TransactionEvent> eventCaptor = ArgumentCaptor.forClass(TransactionEvent.class);
        verify(producer, times(1)).publish(eventCaptor.capture());

        TransactionEvent capturedEvent = eventCaptor.getValue();
        assertEquals(savedTx.getId(), capturedEvent.transactionId());
        assertEquals("SUCCESS", capturedEvent.status());
    }

    @Test
    void processWithdraw_Success() {
        // Arrange
        Long accountId = 2L;
        BigDecimal amount = new BigDecimal("200.00");
        String username = "vinod";

        doNothing().when(accountClient).withdraw(accountId, amount);

        // Act
        transactionService.processWithdraw(accountId, amount, username);

        // Assert
        List<Transaction> savedTransactions = transactionRepository.findAll();
        assertEquals(1, savedTransactions.size());
        assertEquals(TransactionType.WITHDRAW, savedTransactions.get(0).getType());
        assertEquals(TransactionStatus.SUCCESS, savedTransactions.get(0).getStatus());

        verify(producer, times(1)).publish(any(TransactionEvent.class));
    }

    @Test
    void withdrawFallback_InsufficientBalance_ThrowsExceptionAndSavesFailedTransaction() {
        // Arrange
        Long accountId = 1L;
        BigDecimal amount = new BigDecimal("10000.00");
        String username = "aman";
        // Simulate Feign throwing a 400 Bad Request exception
        RuntimeException feignException = new RuntimeException("400 Bad Request: Insufficient balance");

        // Act & Assert Exception
        InsufficientBalanceException thrown = assertThrows(
                InsufficientBalanceException.class,
                () -> transactionService.withdrawFallback(accountId, amount, username, feignException)
        );

        assertEquals("Insufficient balance or account inactive", thrown.getMessage());

        // Assert DB State: Should have saved a FAILED transaction
        List<Transaction> savedTransactions = transactionRepository.findAll();
        assertEquals(1, savedTransactions.size());

        Transaction failedTx = savedTransactions.get(0);
        assertEquals(TransactionType.WITHDRAW, failedTx.getType());
        assertEquals(TransactionStatus.FAILED, failedTx.getStatus());
        assertEquals("400 Bad Request: Insufficient balance", failedTx.getFailureReason());

        // Assert Kafka Producer: Should still publish the FAILED event
        verify(producer, times(1)).publish(any(TransactionEvent.class));
    }

    @Test
    void depositFallback_Unauthorized_ThrowsExceptionAndSavesFailedTransaction() {
        // Arrange
        Long accountId = 1L;
        BigDecimal amount = new BigDecimal("100.00");
        String username = "hacker";
        // Simulate Feign throwing a 403 Forbidden exception
        RuntimeException feignException = new RuntimeException("403 Forbidden");

        // Act & Assert Exception
        UnauthorizedAccountAccessException thrown = assertThrows(
                UnauthorizedAccountAccessException.class,
                () -> transactionService.depositFallback(accountId, amount, username, feignException)
        );

        assertEquals("Account not found or inactive", thrown.getMessage());

        // Assert DB State
        List<Transaction> savedTransactions = transactionRepository.findAll();
        assertEquals(1, savedTransactions.size());
        assertEquals(TransactionStatus.FAILED, savedTransactions.get(0).getStatus());
        assertEquals("403 Forbidden", savedTransactions.get(0).getFailureReason());
    }

    @Test
    void getUserTransactions_ReturnsMappedResponse() {
        // Arrange: Pre-populate the H2 database
        Transaction tx1 = Transaction.builder()
                .accountId(1L).username("aman").type(TransactionType.DEPOSIT)
                .amount(new BigDecimal("500")).status(TransactionStatus.SUCCESS).createdAt(LocalDateTime.now())
                .build();
        Transaction tx2 = Transaction.builder()
                .accountId(1L).username("aman").type(TransactionType.WITHDRAW)
                .amount(new BigDecimal("100")).status(TransactionStatus.SUCCESS).createdAt(LocalDateTime.now())
                .build();
        Transaction txOther = Transaction.builder()
                .accountId(2L).username("other_user").type(TransactionType.DEPOSIT)
                .amount(new BigDecimal("100")).status(TransactionStatus.SUCCESS).createdAt(LocalDateTime.now())
                .build();

        transactionRepository.saveAll(List.of(tx1, tx2, txOther));

        // Act
        List<TransactionResponse> responses = transactionService.getUserTransactions("aman");

        // Assert
        assertEquals(2, responses.size()); // Should only return "aman"s transactions
        assertTrue(responses.stream().anyMatch(r -> r.type().equals("DEPOSIT")));
        assertTrue(responses.stream().anyMatch(r -> r.type().equals("WITHDRAW")));
    }
}