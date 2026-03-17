package com.bank.authservice.dto;

public record AuthResponse(
        String accessToken, String refreshToken
) {}