package com.cybertrace.m2.exception;

/**
 * Exception thrown when evidence processing fails due to server-side or I/O failure.
 */
public class EvidenceProcessingException extends RuntimeException {

    public EvidenceProcessingException(String message) {
        super(message);
    }

    public EvidenceProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
