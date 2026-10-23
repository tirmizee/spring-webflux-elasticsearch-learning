package com.example.learning.documentcrud.exception;

/** Raised when a product with the requested id does not exist. Maps to HTTP 404. */
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String id) {
        super("Product not found: " + id);
    }
}
