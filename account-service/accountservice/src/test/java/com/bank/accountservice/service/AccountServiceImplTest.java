package com.bank.accountservice.service;

import com.bank.accountservice.dto.BalanceResponse;
import com.bank.accountservice.dto.CreateAccountRequest;
import com.bank.accountservice.entity.Account;
import com.bank.accountservice.enums.AccountStatus;
import com.bank.accountservice.enums.AccountType;
import com.bank.accountservice.exception.UnauthorizedAccountAccessException;
import com.bank.accountservice.repository.AccountRepository;
import com.bank.accountservice.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private Account testAccount;
    private final String TEST_USER = "aman";

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .id(1L)
                .accountNumber("ACC-12345")
                .ownerUsername(TEST_USER)
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .frozen(false)
                .build();
    }

    @Test
    void shouldCreateAccountWhenValidRequest() {
        // Arrange
        CreateAccountRequest request = new CreateAccountRequest(AccountType.SAVINGS.name(), new BigDecimal("500.00"));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // Act
        Account result = accountService.createAccount(request, TEST_USER);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_USER, result.getOwnerUsername());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void shouldDepositWhenValidAccountAndAmount() {
        // Arrange: Simulate that the custom @Modifying query successfully updated 1 row
        when(accountRepository.deposit(1L, new BigDecimal("500.00"), TEST_USER)).thenReturn(1);

        // Act & Assert: The method returns void, so we just ensure no exception is thrown
        assertDoesNotThrow(() -> accountService.deposit(1L, new BigDecimal("500.00"), TEST_USER));
    }

    @Test
    void deposit_WhenAccountNotFound_ThrowsUnauthorizedException() {
        // Arrange: Simulate the custom query returning 0 updated rows
        when(accountRepository.deposit(1L, new BigDecimal("500.00"), TEST_USER)).thenReturn(0);

        // Act & Assert
        assertThrows(UnauthorizedAccountAccessException.class,
                () -> accountService.deposit(1L, new BigDecimal("500.00"), TEST_USER));
    }

    @Test
    void shouldWithdrawWhenSufficientBalance() {
        // Arrange: Simulate sufficient balance and successful update
        when(accountRepository.withdrawIfSufficient(1L, new BigDecimal("200.00"), TEST_USER)).thenReturn(1);

        // Act & Assert
        assertDoesNotThrow(() -> accountService.withdraw(1L, new BigDecimal("200.00"), TEST_USER));
    }

    @Test
    void ShouldThrowUnauthorizedExceptionWhenWithdrawDuringInsufficientBalance() {
        // Arrange: Simulate insufficient balance (query returns 0 updated rows)
        when(accountRepository.withdrawIfSufficient(1L, new BigDecimal("2000.00"), TEST_USER)).thenReturn(0);

        // Act & Assert
        assertThrows(UnauthorizedAccountAccessException.class,
                () -> accountService.withdraw(1L, new BigDecimal("2000.00"), TEST_USER));

        verify(accountRepository, times(1)).withdrawIfSufficient(1L, new BigDecimal("2000.00"), TEST_USER);
    }

    @Test
    void shouldReturnBalanceWhenAccountExists() {
        // Arrange
        when(accountRepository.findByIdAndOwnerUsername(1L, TEST_USER)).thenReturn(Optional.of(testAccount));

        // Act
        BalanceResponse response = accountService.getBalance(1L, TEST_USER);

        // Assert
        assertNotNull(response);
        assertEquals(new BigDecimal("1000.00"), response.balance());
    }

    @Test
    void shouldThrowUnauthorizedAccountAccessExceptionWhenUserNameDoesNotMatch() {
        // Arrange
        when(accountRepository.findByIdAndOwnerUsername(1L, TEST_USER)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UnauthorizedAccountAccessException.class,
                () -> accountService.getBalance(1L, TEST_USER));
    }

    @Test
    void shouldFreezeAccountSuccessfully() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // Act
        accountService.freezeAccount(1L);

        // Assert
        assertEquals(AccountStatus.FROZEN, testAccount.getStatus());
        assertTrue(testAccount.isFrozen());
        verify(accountRepository, times(1)).save(testAccount);
    }

    @Test
    void shouldUnfreezeAccountSuccessfully() {
        // Arrange
        // Let's modify our test account to be frozen first
        testAccount.setStatus(AccountStatus.FROZEN);
        testAccount.setFrozen(true);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // Act
        accountService.unfreezeAccount(1L);

        // Assert
        assertEquals(AccountStatus.ACTIVE, testAccount.getStatus());
        assertFalse(testAccount.isFrozen());
        verify(accountRepository, times(1)).save(testAccount);
    }

    @Test
    void shouldReturnAllAccountsForUser() {
        // Arrange
        when(accountRepository.findByOwnerUsername(TEST_USER)).thenReturn(java.util.List.of(testAccount));

        // Act
        var responses = accountService.getAllAccountsForUser(TEST_USER);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(TEST_USER, responses.get(0).ownerName());
        assertEquals(new BigDecimal("1000.00"), responses.get(0).amount());
    }

    @Test
    void shouldThrowExceptionWhenUserHasNoAccounts() {
        // Arrange
        when(accountRepository.findByOwnerUsername(TEST_USER)).thenReturn(java.util.Collections.emptyList());

        // Act & Assert
        assertThrows(com.bank.accountservice.exception.AccountNotFoundException.class,
                () -> accountService.getAllAccountsForUser(TEST_USER));
    }
}