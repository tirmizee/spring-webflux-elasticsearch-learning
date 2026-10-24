package com.example.learning.filtering.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * A {@code terms} filter clause: matches documents whose keyword {@code field} equals any of the
 * given {@code values} (logical OR within the set). Used for both {@code terms} filters in the
 * {@code filter} context and the {@code mustNot} exclusions.
 *
 * <p>{@code values} must be non-empty, and every element must be non-blank. The
 * {@code List<@NotBlank String>} container-element constraint rejects {@code null}, empty, and
 * whitespace-only elements; it is activated by the {@code List<@Valid TermsFilter>} declarations in
 * {@link FilterSearchRequest}, so both {@code terms} and {@code mustNotTerms} are covered.
 *
 * @param field  the keyword field to filter (e.g. "category", "brand")
 * @param values one or more exact, non-blank values
 */
public record TermsFilter(
        @NotBlank(message = "field is required")
        String field,

        @NotEmpty(message = "values must not be empty")
        List<@NotBlank(message = "values must not contain blank entries") String> values
) {
}
