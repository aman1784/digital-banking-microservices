package com.bank.accountservice.exception;

public class UnauthorizedAccountAccessException extends RuntimeException{
    public UnauthorizedAccountAccessException(String message) {
        super(message);
    }
}
