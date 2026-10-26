package com.cybertrace.m2.exception;

/**
 * Exception thrown when an evidence file is invalid, corrupt, or has unrecognized schema headers.
 */
public class InvalidEvidenceFileException extends RuntimeException {

    public InvalidEvidenceFileException(String message) {
        super(message);
    }

    public InvalidEvidenceFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
