package com.example.learning.documentcrud.dto;

import com.example.learning.documentcrud.domain.Product;

import java.math.BigDecimal;

/**
 * Response body for product endpoints. {@code id} is the Elasticsearch document {@code _id};
 * the rest is the {@code _source}. {@code client} identifies which approach served the request.
 */
public record ProductResponse(
        String client,
        String id,
        String name,
        String description,
        BigDecimal price,
        Integer quantity
) {
    public static ProductResponse of(String client, Product product) {
        return new ProductResponse(
                client,
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity()
        );
    }
}
