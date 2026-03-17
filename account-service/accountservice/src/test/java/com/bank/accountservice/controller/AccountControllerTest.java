package com.bank.accountservice.controller;

import com.bank.accountservice.dto.AccountResponse;
import com.bank.accountservice.dto.BalanceResponse;
import com.bank.accountservice.dto.CreateAccountRequest;
import com.bank.accountservice.entity.Account;
import com.bank.accountservice.enums.AccountStatus;
import com.bank.accountservice.enums.AccountType;
import com.bank.accountservice.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false) // Disables Spring Security filters so we can focus on controller logic
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    private final String TEST_USER = "aman";

    @Test
    void shouldCreateAccountSuccessfully() throws Exception {
        // Arrange
        CreateAccountRequest request = new CreateAccountRequest(AccountType.SAVINGS.name(), new BigDecimal("1000.00"));
        Account mockAccount = Account.builder()
                .id(1L)
                .accountNumber("ACC-123")
                .ownerUsername(TEST_USER)
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountService.createAccount(any(CreateAccountRequest.class), eq(TEST_USER))).thenReturn(mockAccount);

        // Act & Assert
        mockMvc.perform(post("/api/v1/accounts")
                        .header("X-User-Name", TEST_USER) // Passing the required header
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("ACC-123"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    void shouldProcessDepositSuccessfully() throws Exception {
        // Arrange
        doNothing().when(accountService).deposit(eq(1L), any(BigDecimal.class), eq(TEST_USER));

        // Act & Assert
        mockMvc.perform(post("/api/v1/accounts/1/deposit")
                        .header("X-User-Name", TEST_USER)
                        .param("amount", "500.00")) // Testing @RequestParam
                .andExpect(status().isOk())
                .andExpect(content().string("Deposit successful"));
    }

    @Test
    void shouldProcessWithdrawSuccessfully() throws Exception {
        // Arrange
        doNothing().when(accountService).withdraw(eq(1L), any(BigDecimal.class), eq(TEST_USER));

        // Act & Assert
        mockMvc.perform(post("/api/v1/accounts/1/withdraw")
                        .header("X-User-Name", TEST_USER)
                        .param("amount", "200.00"))
                .andExpect(status().isOk())
                .andExpect(content().string("Withdraw successful"));
    }

    @Test
    void shouldReturnBalanceSuccessfully() throws Exception {
        // Arrange
        BalanceResponse mockResponse = new BalanceResponse(1L, "ACC-123", "SAVINGS", new BigDecimal("1500.00"), "ACTIVE");

        when(accountService.getBalance(1L, TEST_USER)).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/accounts/1/balance")
                        .header("X-User-Name", TEST_USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1500.00))
                .andExpect(jsonPath("$.accountNumber").value("ACC-123"));
    }

    @Test
    void shouldFreezeAccountSuccessfully() throws Exception {
        // Arrange
        doNothing().when(accountService).freezeAccount(1L);

        // Act & Assert
        mockMvc.perform(put("/api/v1/accounts/admin/accounts/1/freeze")) // No header needed for this in the test since security is bypassed
                .andExpect(status().isOk())
                .andExpect(content().string("Account frozen successfully"));
    }

    @Test
    void shouldReturnAllAccountsForUser() throws Exception {
        // Arrange
        AccountResponse response = new AccountResponse(TEST_USER, AccountType.SAVINGS, new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        when(accountService.getAllAccountsForUser(TEST_USER)).thenReturn(List.of(response));

        // Act & Assert
        mockMvc.perform(get("/api/v1/accounts/me")
                        .header("X-User-Name", TEST_USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ownerName").value(TEST_USER)) // Notice $[0] because it returns a List
                .andExpect(jsonPath("$[0].amount").value(1000.00));
    }
}