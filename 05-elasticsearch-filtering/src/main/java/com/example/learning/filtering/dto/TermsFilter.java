package com.example.learning.filtering.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * A {@code terms} filter clause: matches documents whose keyword {@code field} equals any of the
 * given {@code values} (logical OR within the set). Used for both {@code terms} filters in the
 * {@code filter} context and the {@code mustNot} exclusions.
 *
 * @param field  the keyword field to filter (e.g. "category", "brand")
 * @param values one or more exact values; must be non-empty
 */
public record TermsFilter(
        @NotBlank(message = "field is required")
        String field,

        @NotEmpty(message = "values must not be empty")
        List<String> values
) {
}
