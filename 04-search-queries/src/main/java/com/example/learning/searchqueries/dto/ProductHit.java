package com.example.learning.searchqueries.dto;

import com.example.learning.searchqueries.domain.Product;

import java.math.BigDecimal;

/**
 * One search hit. {@code id} is the Elasticsearch document {@code _id}; {@code score} is the
 * Lucene relevance {@code _score} for the matched query ({@code null}/0 is possible for some
 * query types). The product fields are the {@code _source}.
 */
public record ProductHit(
        String id,
        String name,
        String category,
        String brand,
        String description,
        BigDecimal price,
        Integer quantity,
        Float score
) {
    public static ProductHit of(String id, Float score, Product p) {
        return new ProductHit(
                id, p.getName(), p.getCategory(), p.getBrand(),
                p.getDescription(), p.getPrice(), p.getQuantity(), score);
    }
}
