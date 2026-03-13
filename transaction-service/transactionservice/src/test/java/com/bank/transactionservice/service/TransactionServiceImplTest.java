package com.bank.transactionservice.service;

import com.bank.transactionservice.dto.TransactionResponse;
import com.bank.transactionservice.entity.Transaction;
import com.bank.transactionservice.enums.TransactionStatus;
import com.bank.transactionservice.enums.TransactionType;
import com.bank.transactionservice.executorService.DepositExecutorService;
import com.bank.transactionservice.executorService.WithdrawExecutorService;
import com.bank.transactionservice.feignclient.AccountClient;
import com.bank.transactionservice.kafka.TransactionEventProducer;
import com.bank.transactionservice.repository.TransactionRepository;
import com.bank.transactionservice.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Initializes Mockito annotations
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository repository;

    @Mock
    private AccountClient accountClient;

    @Mock
    private TransactionEventProducer producer;

    @Mock
    private WithdrawExecutorService withdrawExecutorService;

    @Mock
    private DepositExecutorService depositExecutorService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Transaction testTransaction;
    private final String TEST_USER = "aman";
    private final Long TEST_ACCOUNT_ID = 1L;
    private final BigDecimal TEST_AMOUNT = new BigDecimal("500.00");

    @BeforeEach
    void setUp() {

        // Prepare a reusable transaction entity for our tests
        testTransaction = Transaction.builder()
                .id(100L)
                .accountId(TEST_ACCOUNT_ID)
                .username(TEST_USER)
                .type(TransactionType.DEPOSIT)
                .amount(TEST_AMOUNT)
                .status(TransactionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void processDeposit_Success() {
        // Arrange
        // When repository.save() is called, simply return the transaction that was passed into it.
        // This handles both the initial PENDING save and the subsequent SUCCESS save.
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        transactionService.processDeposit(TEST_ACCOUNT_ID, TEST_AMOUNT, TEST_USER);

        // Assert

        // 1. Verify repository.save() was called 1 (for PENDING)
        verify(repository, times(1)).save(any(Transaction.class));
        // 2. Verify depositExecutorService
        verify(depositExecutorService, times(1)).depositInternal(any(), eq(TEST_ACCOUNT_ID), eq(TEST_AMOUNT), eq(TEST_USER));
    }

    @Test
    void processWithdraw_Success() {
        // Arrange
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        transactionService.processWithdraw(TEST_ACCOUNT_ID, TEST_AMOUNT, TEST_USER);

        // Assert
        // Verify Feign client withdraw method was called
        verify(repository, times(1)).save(any(Transaction.class));
        verify(withdrawExecutorService, times(1)).withdrawInternal(any(), eq(TEST_ACCOUNT_ID), eq(TEST_AMOUNT), eq(TEST_USER));

    }

    @Test
    void getUserTransactions_ReturnsPagedResults() {
        // Arrange
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        // Create a fake page of transactions
        Page<Transaction> transactionPage = new PageImpl<>(List.of(testTransaction));
        when(repository.findByUsername(TEST_USER, pageable)).thenReturn(transactionPage);

        // Act
        Page<TransactionResponse> result = transactionService.getUserTransactions(TEST_USER, page, size);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(TEST_AMOUNT, result.getContent().get(0).amount());

        // Verify repository method was called with the exact parameters
        verify(repository, times(1)).findByUsername(TEST_USER, pageable);
    }

    @Test
    void getAccountTransactions_ReturnsPagedResults() {
        // Arrange
        int page = 0;
        int size = 5;
        Pageable pageable = PageRequest.of(page, size);

        Page<Transaction> transactionPage = new PageImpl<>(List.of(testTransaction));
        when(repository.findByAccountIdAndUsername(TEST_ACCOUNT_ID, TEST_USER, pageable)).thenReturn(transactionPage);

        // Act
        Page<TransactionResponse> result = transactionService.getAccountTransactions(TEST_ACCOUNT_ID, TEST_USER, page, size);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(TransactionType.DEPOSIT.name(), result.getContent().get(0).type());
    }
}