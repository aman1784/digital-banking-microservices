package com.bank.accountservice.dto;

import java.math.BigDecimal;

public record BalanceResponse(
        Long accountId,
        String accountNumber,
        String accountType,
        BigDecimal balance,
        String status
) {}