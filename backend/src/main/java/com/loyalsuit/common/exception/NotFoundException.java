package com.loyalsuit.common.exception;

public class NotFoundException extends BusinessException {

    public NotFoundException(String resource, Object id) {
        super(resource + " not found: " + id, org.springframework.http.HttpStatus.NOT_FOUND);
    }
}
