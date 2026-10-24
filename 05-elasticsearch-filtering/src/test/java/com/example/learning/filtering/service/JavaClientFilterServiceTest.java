package com.example.learning.filtering.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import com.example.learning.filtering.domain.Product;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Java API Client filter service: hit mapping, filter-context score
 * normalization, empty results, and blocking-scheduler isolation. The {@link ElasticsearchClient}
 * is mocked.
 */
class JavaClientFilterServiceTest {

    // Build REAL co.elastic client objects (Hit/SearchResponse) rather than mocking them: their
    // accessors are final methods the mock maker cannot stub.
    private static SearchResponse<Product> responseWith(List<Hit<Product>> hits) {
        return SearchResponse.of(r -> r
                .took(1L)
                .timedOut(false)
                .shards(s -> s.total(1).successful(1).failed(0))
                .hits(h -> h
                        .total(t -> t.value(hits.size()).relation(TotalHitsRelation.Eq))
                        .hits(hits)));
    }

    private static Hit<Product> hit(String id, Double score, Product source) {
        return Hit.of(h -> h.index("filter-products").id(id).score(score).source(source));
    }

    private static Product product(String id) {
        return new Product(id, "Apple MacBook Pro", "laptop", "Apple",
                new BigDecimal("1999.00"), 4.8, LocalDate.parse("2024-03-10"), true, 10);
    }

    @Test
    @SuppressWarnings("unchecked")
    void mapsHitsToResponse() throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        when(client.search(any(Function.class), any(Class.class)))
                .thenReturn(responseWith(List.of(hit("p01", null, product("p01")))));

        StepVerifier.create(new JavaClientFilterService(client).term("category", "laptop"))
                .assertNext(resp -> {
                    assertEquals("java-client", resp.client());
                    assertEquals(1, resp.total());
                    assertEquals("p01", resp.hits().get(0).id());
                    assertEquals("laptop", resp.hits().get(0).category());
                    assertEquals(true, resp.hits().get(0).inStock());
                })
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void filterContextScoreNormalizedToZero() throws Exception {
        // A pure filter returns a null score from Elasticsearch; the service surfaces it as 0.0.
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        when(client.search(any(Function.class), any(Class.class)))
                .thenReturn(responseWith(List.of(hit("p01", null, product("p01")))));

        StepVerifier.create(new JavaClientFilterService(client).exists("discountPercent"))
                .assertNext(resp -> assertEquals(0.0f, resp.hits().get(0).score()))
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void emptyResultYieldsEmptyList() throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        when(client.search(any(Function.class), any(Class.class)))
                .thenReturn(responseWith(List.of()));

        StepVerifier.create(new JavaClientFilterService(client).terms("brand", List.of("Nonexistent")))
                .assertNext(resp -> {
                    assertEquals(0, resp.total());
                    assertTrue(resp.hits().isEmpty());
                })
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchRunsOnBoundedElasticNotCallerThread() throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        AtomicReference<String> thread = new AtomicReference<>();
        when(client.search(any(Function.class), any(Class.class))).thenAnswer(inv -> {
            thread.set(Thread.currentThread().getName());
            return responseWith(List.of());
        });

        String caller = Thread.currentThread().getName();
        StepVerifier.create(new JavaClientFilterService(client).term("category", "laptop"))
                .expectNextCount(1)
                .verifyComplete();

        String actual = thread.get();
        assertNotNull(actual, "search() was never invoked");
        assertTrue(actual.startsWith("boundedElastic-"),
                "blocking search must run on a boundedElastic worker but ran on: " + actual);
        assertNotEquals(caller, actual, "blocking search must not run on the caller thread");
    }
}
