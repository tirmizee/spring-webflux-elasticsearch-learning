package com.example.learning.filtering.query;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.learning.filtering.dto.FilterSearchRequest;
import com.example.learning.filtering.dto.RangeFilter;
import com.example.learning.filtering.dto.TermsFilter;
import com.example.learning.filtering.exception.InvalidFilterException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the filter Query DSL construction (the shared core both clients use). These assert the
 * type-safe structure of the built {@code co.elastic.clients} queries, not mocked return values.
 */
class ProductFiltersTest {

    @Test
    void termBuildsTermQueryOnKeywordField() {
        Query q = ProductFilters.term("category", "laptop");
        assertTrue(q.isTerm());
        assertEquals("category", q.term().field());
        assertEquals("laptop", q.term().value().stringValue());
    }

    @Test
    void termBooleanBuildsBooleanTermQuery() {
        Query q = ProductFilters.term("inStock", true);
        assertTrue(q.isTerm());
        assertEquals("inStock", q.term().field());
        assertTrue(q.term().value().booleanValue());
    }

    @Test
    void termsBuildsTermsQueryWithAllValues() {
        Query q = ProductFilters.terms("brand", List.of("Apple", "Dell"));
        assertTrue(q.isTerms());
        assertEquals("brand", q.terms().field());
        List<String> values = q.terms().terms().value().stream()
                .map(fv -> fv.stringValue())
                .toList();
        assertEquals(List.of("Apple", "Dell"), values);
    }

    @Test
    void existsBuildsExistsQueryOnField() {
        Query q = ProductFilters.exists("discountPercent");
        assertTrue(q.isExists());
        assertEquals("discountPercent", q.exists().field());
    }

    @Test
    void numberRangeBuildsInclusiveAndExclusiveBounds() {
        Query q = ProductFilters.range(new RangeFilter("price", "number", "1000", null, null, "2000"));
        assertTrue(q.isRange());
        assertTrue(q.range().isNumber());
        assertEquals(1000.0, q.range().number().gte());
        assertEquals(2000.0, q.range().number().lt());
    }

    @Test
    void dateRangeBuildsDateVariantWithFormat() {
        Query q = ProductFilters.range(new RangeFilter("releaseDate", "date", "2024-01-01", "2024-12-31", null, null));
        assertTrue(q.isRange());
        assertTrue(q.range().isDate());
        assertEquals("releaseDate", q.range().date().field());
        assertEquals("2024-01-01", q.range().date().gte());
        assertEquals("2024-12-31", q.range().date().lte());
        assertEquals("yyyy-MM-dd", q.range().date().format());
    }

    @Test
    void rangeWithoutAnyBoundThrowsInvalidFilter() {
        RangeFilter noBounds = new RangeFilter("price", "number", null, null, null, null);
        assertThrows(InvalidFilterException.class, () -> ProductFilters.range(noBounds));
    }

    @Test
    void numericRangeWithNonNumericBoundThrowsInvalidFilter() {
        RangeFilter bad = new RangeFilter("price", "number", "not-a-number", null, null, null);
        assertThrows(InvalidFilterException.class, () -> ProductFilters.range(bad));
    }

    @Test
    void filterContextWrapsClausesInBoolFilter() {
        Query q = ProductFilters.filterContext(List.of(
                ProductFilters.term("category", "laptop"),
                ProductFilters.exists("discountPercent")));
        assertTrue(q.isBool());
        BoolQuery bool = q.bool();
        assertEquals(2, bool.filter().size());
        assertTrue(bool.must().isEmpty(), "pure filter context must have no scored must clauses");
    }

    @Test
    void composeWithoutQueryPutsEverythingInFilterContext() {
        FilterSearchRequest request = new FilterSearchRequest(
                null,
                List.of(new TermsFilter("category", List.of("laptop", "monitor"))),
                List.of(new RangeFilter("price", "number", "300", "2000", null, null)),
                List.of("discountPercent"),
                List.of(new TermsFilter("brand", List.of("Apple"))));

        BoolQuery bool = ProductFilters.compose(request).bool();
        assertTrue(bool.must().isEmpty(), "no full-text query -> no must (filter context only)");
        assertEquals(3, bool.filter().size(), "terms + range + exists -> 3 filter clauses");
        assertEquals(1, bool.mustNot().size());
    }

    @Test
    void composeWithQueryAddsScoredMustClause() {
        FilterSearchRequest request = new FilterSearchRequest(
                "gaming",
                List.of(new TermsFilter("category", List.of("laptop"))),
                null, null, null);

        BoolQuery bool = ProductFilters.compose(request).bool();
        assertEquals(1, bool.must().size(), "full-text query -> one scored must clause");
        assertTrue(bool.must().get(0).isMatch());
        assertEquals("name", bool.must().get(0).match().field());
        assertEquals(1, bool.filter().size());
        assertFalse(bool.filter().isEmpty());
    }
}
