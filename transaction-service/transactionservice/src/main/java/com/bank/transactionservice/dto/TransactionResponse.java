package com.bank.transactionservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        Long accountId,
        String type,
        BigDecimal amount,
        String status,
        String failureReason,
        LocalDateTime createdAt
) {}