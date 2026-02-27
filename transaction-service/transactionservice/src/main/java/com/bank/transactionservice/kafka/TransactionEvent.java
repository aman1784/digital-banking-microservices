package com.bank.transactionservice.kafka;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionEvent(
        Long transactionId,
        Long accountId,
        String username,
        String type,
        String status,
        BigDecimal amount,
        LocalDateTime timestamp
) {}