package com.bank.authservice.dto;

public record LoginRequest(
        String username,
        String password
) {}