package com.bank.authservice.exception;

public class DuplicateUserExistsException extends RuntimeException{
    public DuplicateUserExistsException(String message) {
        super(message);
    }
}
