package com.bank.transactionservice.controller;

import com.bank.transactionservice.dto.TransactionResponse;
import com.bank.transactionservice.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class) // Only loads the web layer for this specific controller
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc; // Used to simulate HTTP requests

    @MockBean
    private TransactionService transactionService; // Mocks the service layer

    @Test
    void deposit_ShouldReturnOk_WhenRequestIsValid() throws Exception {
        // Arrange
        Long accountId = 1L;
        BigDecimal amount = new BigDecimal("100.00");
        String username = "aman";

        // Mock the service to do nothing (since it returns void)
        doNothing().when(transactionService).processDeposit(anyLong(), any(BigDecimal.class), anyString());

        // Create the JSON payload string
        String jsonPayload = "{\"amount\": 100.00}";

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions/{accountId}/deposit", accountId)
                        .header("X-User-Name", username)
                        .content(jsonPayload)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Transaction completed"));
    }

    @Test
    void withdraw_ShouldReturnOk_WhenRequestIsValid() throws Exception {
        // Arrange
        Long accountId = 1L;
        BigDecimal amount = new BigDecimal("50.00");
        String username = "aman";

        // Mock the service
        doNothing().when(transactionService).processWithdraw(anyLong(), any(BigDecimal.class), anyString());

        // Create the JSON payload string
        String jsonPayload = "{\"amount\": 50.00}";

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions/{accountId}/withdraw", accountId)
                        .header("X-User-Name", username)
                        .content(jsonPayload)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Transaction completed"));
    }

    @Test
    void getMyTransactions_ShouldReturnListOfTransactions() throws Exception {
        // Arrange
        String username = "aman";
        TransactionResponse response1 = new TransactionResponse(
                1L, 1L, "DEPOSIT", new BigDecimal("100.00"), "SUCCESS", null, LocalDateTime.now()
        );
        TransactionResponse response2 = new TransactionResponse(
                2L, 1L, "WITHDRAW", new BigDecimal("50.00"), "SUCCESS", null, LocalDateTime.now()
        );

        when(transactionService.getUserTransactions(username)).thenReturn(List.of(response1, response2));

        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions/me")
                        .header("X-User-Name", username)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[1].type").value("WITHDRAW"));
    }

    @Test
    void getAccountTransactions_ShouldReturnListOfTransactions() throws Exception {
        // Arrange
        Long accountId = 1L;
        String username = "aman";
        TransactionResponse response = new TransactionResponse(
                1L, accountId, "DEPOSIT", new BigDecimal("500.00"), "SUCCESS", null, LocalDateTime.now()
        );

        when(transactionService.getAccountTransactions(accountId, username)).thenReturn(List.of(response));

        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions/account/{accountId}", accountId)
                        .header("X-User-Name", username)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].accountId").value(accountId))
                .andExpect(jsonPath("$[0].amount").value(500.00));
    }
}