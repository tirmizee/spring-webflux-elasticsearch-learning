package com.example.learning.searchqueries.dto;

import jakarta.validation.Valid;

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
 */
public record BoolSearchRequest(
        List<@Valid QueryClause> must,
        List<@Valid QueryClause> should,
        List<@Valid QueryClause> mustNot,
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
