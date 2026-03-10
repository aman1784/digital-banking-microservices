package com.bank.accountservice.dto;

import com.bank.accountservice.enums.AccountStatus;
import com.bank.accountservice.enums.AccountType;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AccountResponse (String ownerName,
                               AccountType accountType,
                               BigDecimal amount,
                               AccountStatus accountStatus) {}
