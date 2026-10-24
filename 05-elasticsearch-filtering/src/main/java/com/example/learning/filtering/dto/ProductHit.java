package com.example.learning.filtering.dto;

import com.example.learning.filtering.domain.Product;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One search hit. {@code id} is the Elasticsearch document {@code _id}; {@code score} is the
 * Lucene relevance {@code _score}. In pure <strong>filter context</strong> the score is a constant
 * {@code 0.0} (filters do not contribute to relevance); when a full-text query is present the
 * matching documents get a positive score. The remaining fields are the {@code _source}.
 */
public record ProductHit(
        String id,
        String name,
        String category,
        String brand,
        BigDecimal price,
        Double rating,
        LocalDate releaseDate,
        Boolean inStock,
        Integer discountPercent,
        Float score
) {
    public static ProductHit of(String id, Float score, Product p) {
        return new ProductHit(
                id,
                p.getName(),
                p.getCategory(),
                p.getBrand(),
                p.getPrice(),
                p.getRating(),
                p.getReleaseDate(),
                p.getInStock(),
                p.getDiscountPercent(),
                score
        );
    }
}
