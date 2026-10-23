package com.example.learning.searchqueries.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * A single leaf clause inside a bool query. This is a small, teachable subset of the Elasticsearch
 * Query DSL (we deliberately do NOT expose the raw DSL as the public API).
 *
 * @param type  "term" (exact, keyword fields) or "match" (full-text, analyzed text fields)
 * @param field the document field to query (e.g. "category", "brand", "name", "description")
 * @param value the value/text to search for
 */
public record QueryClause(
        @NotNull(message = "type is required")
        @Pattern(regexp = "term|match", message = "type must be 'term' or 'match'")
        String type,

        @NotBlank(message = "field is required")
        String field,

        @NotBlank(message = "value is required")
        String value
) {
}
