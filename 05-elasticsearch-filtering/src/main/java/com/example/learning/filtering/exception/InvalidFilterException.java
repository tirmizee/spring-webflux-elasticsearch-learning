package com.example.learning.filtering.exception;

/**
 * Thrown when a filter request is structurally valid (passes bean validation) but semantically
 * invalid — e.g. a range with no bounds, or a numeric bound that is not a number. Mapped to
 * HTTP 400 by {@link GlobalExceptionHandler} before any Elasticsearch call.
 */
public class InvalidFilterException extends RuntimeException {

    public InvalidFilterException(String message) {
        super(message);
    }
}
