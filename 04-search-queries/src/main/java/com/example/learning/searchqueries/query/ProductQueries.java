package com.example.learning.searchqueries.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.learning.searchqueries.dto.BoolSearchRequest;
import com.example.learning.searchqueries.dto.QueryClause;

import java.util.List;

/**
 * Builds type-safe Elasticsearch {@link Query} objects from the chapter's small DTO vocabulary.
 *
 * <p>This is shared by BOTH clients on purpose: Spring Data's {@code NativeQuery} and the official
 * Java API Client both consume {@code co.elastic.clients} {@link Query} objects, so the two
 * approaches run the <em>identical</em> query and differ only in how they execute it. Keeping the
 * query construction in one place is what makes the comparison meaningful — it is a focused helper,
 * not an architectural abstraction (no interface, no state).
 */
public final class ProductQueries {

    private ProductQueries() {
    }

    /**
     * Exact indexed-term match. Use on {@code keyword} fields (e.g. {@code category}, {@code brand}).
     * On an analyzed {@code text} field a term query matches individual lowercased tokens, which is
     * usually not what you want — that is why exact matching uses keyword fields.
     */
    public static Query term(String field, String value) {
        return Query.of(q -> q.term(t -> t.field(field).value(value)));
    }

    /**
     * Full-text match. The query text is analyzed the same way as the field at index time, so
     * {@code match} on {@code name}/{@code description} matches on tokens and contributes to
     * relevance ({@code _score}).
     */
    public static Query match(String field, String text) {
        return Query.of(q -> q.match(m -> m.field(field).query(text)));
    }

    /** Builds a single leaf query from a {@link QueryClause} (term or match). */
    public static Query clause(QueryClause c) {
        return "term".equals(c.type()) ? term(c.field(), c.value()) : match(c.field(), c.value());
    }

    /**
     * Composes a bool query from the request. {@code must}/{@code should}/{@code mustNot} map
     * directly to the bool clauses; {@code minimumShouldMatch} is applied only when provided.
     */
    public static Query bool(BoolSearchRequest request) {
        List<Query> must = request.mustOrEmpty().stream().map(ProductQueries::clause).toList();
        List<Query> should = request.shouldOrEmpty().stream().map(ProductQueries::clause).toList();
        List<Query> mustNot = request.mustNotOrEmpty().stream().map(ProductQueries::clause).toList();

        return Query.of(q -> q.bool(b -> {
            b.must(must).should(should).mustNot(mustNot);
            if (request.minimumShouldMatch() != null) {
                b.minimumShouldMatch(String.valueOf(request.minimumShouldMatch()));
            }
            return b;
        }));
    }
}
