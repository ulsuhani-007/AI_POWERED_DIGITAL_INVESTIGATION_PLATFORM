package com.cybertrace.m1.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceName, String field, String value) {
        super(resourceName + " already exists with " + field + ": " + value);
    }
}

