package com.example.learning.filtering.dto;

import jakarta.validation.Valid;

import java.util.List;
// Note: @Valid on the list type argument (List<@Valid X>) validates each nested element, per the
// Bean Validation container-element convention.

/**
 * Structured input for a composed filter search. This is where filter context and query context
 * meet in a single request:
 * <ul>
 *     <li>{@code query} — optional full-text {@code match} on {@code name}, placed in
 *         {@code bool.must} (<strong>query context</strong>: contributes to {@code _score}).</li>
 *     <li>{@code terms} — {@code terms} clauses placed in {@code bool.filter}
 *         (<strong>filter context</strong>: no scoring, cacheable), combined with AND.</li>
 *     <li>{@code ranges} — {@code range} clauses placed in {@code bool.filter} (AND).</li>
 *     <li>{@code exists} — field names placed in {@code bool.filter} as {@code exists} clauses (AND).</li>
 *     <li>{@code mustNotTerms} — {@code terms} clauses placed in {@code bool.must_not} (exclusion).</li>
 * </ul>
 * All lists are optional; a null list is treated as empty. When only filters are present (no
 * {@code query}), every hit has {@code score} {@code 0.0}, demonstrating filter context.
 */
public record FilterSearchRequest(
        String query,
        List<@Valid TermsFilter> terms,
        List<@Valid RangeFilter> ranges,
        List<String> exists,
        List<@Valid TermsFilter> mustNotTerms
) {
    public List<TermsFilter> termsOrEmpty() {
        return terms == null ? List.of() : terms;
    }

    public List<RangeFilter> rangesOrEmpty() {
        return ranges == null ? List.of() : ranges;
    }

    public List<String> existsOrEmpty() {
        return exists == null ? List.of() : exists;
    }

    public List<TermsFilter> mustNotTermsOrEmpty() {
        return mustNotTerms == null ? List.of() : mustNotTerms;
    }

    /** True when the request carries a non-blank full-text query (query context present). */
    public boolean hasQuery() {
        return query != null && !query.isBlank();
    }
}
