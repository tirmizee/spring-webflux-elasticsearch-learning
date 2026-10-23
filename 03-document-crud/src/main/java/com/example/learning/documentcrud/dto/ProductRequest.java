package com.example.learning.documentcrud.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request body for create (POST) and update (PUT). The document id is taken from the URL path,
 * not the body, so ids stay explicit and reproducible.
 *
 * @param name        required, non-blank product name
 * @param description optional free-text description
 * @param price       required, must be >= 0
 * @param quantity    required, must be >= 0
 */
public record ProductRequest(
        @NotBlank(message = "name must not be blank")
        String name,

        String description,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.0", message = "price must be >= 0")
        BigDecimal price,

        @NotNull(message = "quantity is required")
        @Min(value = 0, message = "quantity must be >= 0")
        Integer quantity
) {
}
