package com.bank.accountservice.dto;

// import com.bank.accountservice.enums.AccountType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateAccountRequest(
        // @NotNull AccountType accountType,
        @NotNull String accountType,
        @Positive BigDecimal initialDeposit
) {}