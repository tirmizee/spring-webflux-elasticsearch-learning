package com.example.learning.searchqueries.query;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.learning.searchqueries.dto.BoolSearchRequest;
import com.example.learning.searchqueries.dto.QueryClause;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the Query DSL construction (the shared core both clients use). These assert the
 * type-safe structure of the built {@code co.elastic.clients} queries, not mocked return values.
 */
class ProductQueriesTest {

    @Test
    void termBuildsTermQueryOnField() {
        Query q = ProductQueries.term("category", "laptop");
        assertTrue(q.isTerm(), "expected a term query");
        assertEquals("category", q.term().field());
        assertEquals("laptop", q.term().value().stringValue());
    }

    @Test
    void matchBuildsMatchQueryOnField() {
        Query q = ProductQueries.match("name", "macbook");
        assertTrue(q.isMatch(), "expected a match query");
        assertEquals("name", q.match().field());
        assertEquals("macbook", q.match().query().stringValue());
    }

    @Test
    void clauseDispatchesByType() {
        assertTrue(ProductQueries.clause(new QueryClause("term", "brand", "Apple")).isTerm());
        assertTrue(ProductQueries.clause(new QueryClause("match", "description", "wireless")).isMatch());
    }

    @Test
    void boolComposesAllClausesAndMinimumShouldMatch() {
        BoolSearchRequest request = new BoolSearchRequest(
                List.of(new QueryClause("term", "category", "laptop")),
                List.of(new QueryClause("match", "name", "pro"),
                        new QueryClause("match", "description", "gaming")),
                List.of(new QueryClause("term", "brand", "Apple")),
                1);

        Query q = ProductQueries.bool(request);
        assertTrue(q.isBool(), "expected a bool query");
        BoolQuery bool = q.bool();
        assertEquals(1, bool.must().size());
        assertEquals(2, bool.should().size());
        assertEquals(1, bool.mustNot().size());
        assertEquals("1", bool.minimumShouldMatch());
    }

    @Test
    void boolOmitsMinimumShouldMatchWhenNull() {
        BoolSearchRequest request = new BoolSearchRequest(
                List.of(new QueryClause("term", "category", "laptop")), null, null, null);

        BoolQuery bool = ProductQueries.bool(request).bool();
        assertEquals(1, bool.must().size());
        assertTrue(bool.should().isEmpty());
        assertTrue(bool.mustNot().isEmpty());
        assertNull(bool.minimumShouldMatch());
    }
}
