package com.ieltsplatform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when client request fails domain business validation.
 * Translates to HTTP 400 BAD_REQUEST.
 */
public class BadRequestException extends DomainException {

    private static final String DEFAULT_ERROR_CODE = "BAD_REQUEST";

    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST, DEFAULT_ERROR_CODE);
    }

    public BadRequestException(String message, String errorCode) {
        super(message, HttpStatus.BAD_REQUEST, errorCode);
    }
}
