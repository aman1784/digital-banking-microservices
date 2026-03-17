package com.bank.transactionservice.controller;

import com.bank.transactionservice.dto.DepositRequest;
import com.bank.transactionservice.dto.TransactionResponse;
import com.bank.transactionservice.dto.WithdrawRequest;
import com.bank.transactionservice.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false) // Disables security filters for simple unit testing
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    private final String TEST_USER = "aman";

    @Test
    void shouldProcessDepositSuccessfully() throws Exception {
        // Arrange
        DepositRequest request = new DepositRequest(new BigDecimal("500.00"));

        doNothing().when(transactionService).processDeposit(eq(1L), any(BigDecimal.class), eq(TEST_USER));

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions/1/deposit")
                        .header("X-User-Name", TEST_USER) // Simulate the Gateway passing the header
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Transaction completed"));
    }

    @Test
    void shouldProcessWithdrawSuccessfully() throws Exception {
        // Arrange
        WithdrawRequest request = new WithdrawRequest(new BigDecimal("200.00"));

        doNothing().when(transactionService).processWithdraw(eq(1L), any(BigDecimal.class), eq(TEST_USER));

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions/1/withdraw")
                        .header("X-User-Name", TEST_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Transaction completed"));
    }

    @Test
    void shouldReturnMyTransactions() throws Exception {
        // Arrange
        TransactionResponse response = new TransactionResponse(
                100L, 1L, "DEPOSIT", new BigDecimal("500.00"), "SUCCESS", null, LocalDateTime.now()
        );
        Page<TransactionResponse> mockPage = new PageImpl<>(List.of(response));

        when(transactionService.getUserTransactions(eq(TEST_USER), anyInt(), anyInt())).thenReturn(mockPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions/me")
                        .header("X-User-Name", TEST_USER)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].amount").value(500.00)) // Checking the paginated content array
                .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"));
    }

    @Test
    void shouldReturnAccountTransactions() throws Exception {
        // Arrange
        TransactionResponse response = new TransactionResponse(
                100L, 1L, "WITHDRAW", new BigDecimal("200.00"), "SUCCESS", null, LocalDateTime.now()
        );
        Page<TransactionResponse> mockPage = new PageImpl<>(List.of(response));

        when(transactionService.getAccountTransactions(eq(1L), eq(TEST_USER), anyInt(), anyInt())).thenReturn(mockPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions/account/1")
                        .header("X-User-Name", TEST_USER)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].amount").value(200.00))
                .andExpect(jsonPath("$.content[0].type").value("WITHDRAW"));
    }
}