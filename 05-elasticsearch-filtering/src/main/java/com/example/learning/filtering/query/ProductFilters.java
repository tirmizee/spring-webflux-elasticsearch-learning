package com.example.learning.filtering.query;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.learning.filtering.dto.FilterSearchRequest;
import com.example.learning.filtering.dto.RangeFilter;
import com.example.learning.filtering.dto.TermsFilter;
import com.example.learning.filtering.exception.InvalidFilterException;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds type-safe Elasticsearch {@link Query} objects for the chapter's filter vocabulary.
 *
 * <p>Shared by BOTH clients on purpose: Spring Data's {@code NativeQuery} and the official Java API
 * Client both consume {@code co.elastic.clients} {@link Query} objects, so the two approaches run
 * the <em>identical</em> query and differ only in how they execute it. This is a focused, stateless
 * helper — not an architectural abstraction (no interface, no state).
 *
 * <h2>Filter context vs query context</h2>
 * The leaf builders here ({@code term}, {@code terms}, {@code range}, {@code exists}) are placed
 * inside {@code bool.filter} by {@link #filterContext}/{@link #compose}, so they run in
 * <strong>filter context</strong>: they decide yes/no membership, do not affect {@code _score}, and
 * are cacheable. A full-text {@code match} placed in {@code bool.must} runs in
 * <strong>query context</strong> and does contribute to {@code _score}.
 */
public final class ProductFilters {

    private ProductFilters() {
    }

    // ---- leaf clauses ----

    /** Exact {@code term} on a keyword field. */
    public static Query term(String field, String value) {
        return Query.of(q -> q.term(t -> t.field(field).value(value)));
    }

    /** Exact {@code term} on a boolean field (e.g. {@code inStock}). */
    public static Query term(String field, boolean value) {
        return Query.of(q -> q.term(t -> t.field(field).value(value)));
    }

    /** {@code terms} filter — matches any of {@code values} (OR within the set). */
    public static Query terms(String field, List<String> values) {
        List<FieldValue> fieldValues = values.stream().map(FieldValue::of).toList();
        return Query.of(q -> q.terms(t -> t
                .field(field)
                .terms(tf -> tf.value(fieldValues))));
    }

    /** {@code exists} filter — matches documents that have a non-null value for {@code field}. */
    public static Query exists(String field) {
        return Query.of(q -> q.exists(e -> e.field(field)));
    }

    /**
     * {@code range} filter. {@code gte}/{@code lte} are inclusive; {@code gt}/{@code lt} exclusive.
     * The variant is chosen from {@link RangeFilter#type()}, which must be {@code "number"} or
     * {@code "date"}. Throws {@link InvalidFilterException} when the type is unsupported, no bound
     * is set, or a numeric bound is not a finite number.
     *
     * <p>This method is defensive on purpose: even when invoked directly (bypassing the DTO's
     * {@code @Pattern} validation on the compose endpoint), an unsupported type can never silently
     * fall back to a numeric range.
     */
    public static Query range(RangeFilter filter) {
        if (!filter.hasAnyBound()) {
            throw new InvalidFilterException(
                    "range filter on '" + filter.field() + "' must set at least one of gte/lte/gt/lt");
        }
        if ("date".equals(filter.type())) {
            return dateRange(filter);
        }
        if ("number".equals(filter.type())) {
            return numberRange(filter);
        }
        throw new InvalidFilterException(
                "range type must be 'number' or 'date' but was '" + filter.type() + "'");
    }

    private static Query numberRange(RangeFilter f) {
        return Query.of(q -> q.range(r -> r.number(n -> {
            n.field(f.field());
            if (isSet(f.gte())) {
                n.gte(parseDouble(f.field(), f.gte()));
            }
            if (isSet(f.lte())) {
                n.lte(parseDouble(f.field(), f.lte()));
            }
            if (isSet(f.gt())) {
                n.gt(parseDouble(f.field(), f.gt()));
            }
            if (isSet(f.lt())) {
                n.lt(parseDouble(f.field(), f.lt()));
            }
            return n;
        })));
    }

    private static Query dateRange(RangeFilter f) {
        return Query.of(q -> q.range(r -> r.date(d -> {
            d.field(f.field()).format("yyyy-MM-dd");
            if (isSet(f.gte())) {
                d.gte(f.gte());
            }
            if (isSet(f.lte())) {
                d.lte(f.lte());
            }
            if (isSet(f.gt())) {
                d.gt(f.gt());
            }
            if (isSet(f.lt())) {
                d.lt(f.lt());
            }
            return d;
        })));
    }

    // ---- composition ----

    /**
     * Wraps the given leaf clauses as a pure filter-context query: {@code bool { filter: [...] }}.
     * Every matching document gets a constant {@code _score} of {@code 0.0}.
     */
    public static Query filterContext(List<Query> filters) {
        return Query.of(q -> q.bool(b -> b.filter(filters)));
    }

    /**
     * Composes a request into a single {@code bool} query:
     * <ul>
     *     <li>{@code query} (if present) → {@code must} (query context, scored)</li>
     *     <li>{@code terms}/{@code ranges}/{@code exists} → {@code filter} (filter context, unscored)</li>
     *     <li>{@code mustNotTerms} → {@code must_not} (exclusion)</li>
     * </ul>
     */
    public static Query compose(FilterSearchRequest request) {
        List<Query> filters = new ArrayList<>();
        for (TermsFilter t : request.termsOrEmpty()) {
            filters.add(terms(t.field(), t.values()));
        }
        for (RangeFilter r : request.rangesOrEmpty()) {
            filters.add(range(r));
        }
        for (String field : request.existsOrEmpty()) {
            if (field == null || field.isBlank()) {
                throw new InvalidFilterException("exists field must not be blank");
            }
            filters.add(exists(field));
        }

        List<Query> mustNot = new ArrayList<>();
        for (TermsFilter t : request.mustNotTermsOrEmpty()) {
            mustNot.add(terms(t.field(), t.values()));
        }

        return Query.of(q -> q.bool(b -> {
            if (request.hasQuery()) {
                b.must(m -> m.match(mm -> mm.field("name").query(request.query())));
            }
            b.filter(filters).mustNot(mustNot);
            return b;
        }));
    }

    private static boolean isSet(String bound) {
        return bound != null && !bound.isBlank();
    }

    private static double parseDouble(String field, String value) {
        double parsed;
        try {
            parsed = Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new InvalidFilterException(
                    "range bound on '" + field + "' must be numeric but was '" + value + "'");
        }
        // Double.parseDouble accepts "NaN"/"Infinity"/"-Infinity"; these are not valid range bounds.
        if (!Double.isFinite(parsed)) {
            throw new InvalidFilterException(
                    "range bound on '" + field + "' must be a finite number but was '" + value + "'");
        }
        return parsed;
    }
}
