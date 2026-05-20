package com.banny.lotsonote.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST.value(), message);
    }

    public int getCode() {
        return code;
    }
}
