package com.loyalsuit.common.exception;

public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(message, org.springframework.http.HttpStatus.CONFLICT);
    }
}
