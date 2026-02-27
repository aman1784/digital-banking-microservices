package com.bank.authservice.exception;

import java.time.LocalDateTime;

public record ApiError(LocalDateTime timeStamp, int status, String error, String message) {
}