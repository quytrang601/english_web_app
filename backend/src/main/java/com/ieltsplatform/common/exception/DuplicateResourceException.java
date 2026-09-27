package com.ieltsplatform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to create a resource that already exists (e.g. duplicate email).
 * Translates to HTTP 409 CONFLICT.
 */
public class DuplicateResourceException extends DomainException {

    private static final String DEFAULT_ERROR_CODE = "DUPLICATE_RESOURCE";

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT, DEFAULT_ERROR_CODE);
    }

    public DuplicateResourceException(String message, String errorCode) {
        super(message, HttpStatus.CONFLICT, errorCode);
    }
}
