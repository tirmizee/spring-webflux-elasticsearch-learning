package com.example.learning.documentcrud.exception;

/** Raised when creating a product whose id already exists. Maps to HTTP 409. */
public class DuplicateProductException extends RuntimeException {
    public DuplicateProductException(String id) {
        super("Product already exists: " + id);
    }
}
