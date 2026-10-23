package com.example.learning.searchqueries.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import java.util.List;
// Note: @Valid is applied to the list type argument (List<@Valid QueryClause>) so nested clause
// constraints are validated, per the Bean Validation container-element convention.

/**
 * Structured input for a bool query. Each list maps directly to a bool clause:
 * <ul>
 *     <li>{@code must} — all must match (AND); contributes to score</li>
 *     <li>{@code should} — optional matches; when the query also has {@code must}, {@code should}
 *         is purely a relevance booster unless {@code minimumShouldMatch} is set</li>
 *     <li>{@code mustNot} — documents matching any of these are excluded</li>
 *     <li>{@code minimumShouldMatch} — how many {@code should} clauses must match (optional)</li>
 * </ul>
 * All lists are optional; a null list is treated as empty.
 *
 * <p>{@code minimumShouldMatch} is optional. This chapter deliberately exposes only a simplified
 * <strong>non-negative integer</strong> API ({@code null}/0/positive accepted; negative → HTTP 400).
 * Elasticsearch itself supports a richer {@code minimum_should_match} syntax (percentages and
 * negative expressions); that is out of scope here.
 */
public record BoolSearchRequest(
        List<@Valid QueryClause> must,
        List<@Valid QueryClause> should,
        List<@Valid QueryClause> mustNot,
        @Min(value = 0, message = "minimumShouldMatch must be >= 0")
        Integer minimumShouldMatch
) {
    public List<QueryClause> mustOrEmpty() {
        return must == null ? List.of() : must;
    }

    public List<QueryClause> shouldOrEmpty() {
        return should == null ? List.of() : should;
    }

    public List<QueryClause> mustNotOrEmpty() {
        return mustNot == null ? List.of() : mustNot;
    }
}
