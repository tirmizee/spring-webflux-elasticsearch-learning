package com.example.learning.searchqueries.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import com.example.learning.searchqueries.domain.Product;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
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
 * Unit tests for the Java API Client search service: hit mapping, empty results, and
 * blocking-scheduler isolation. The {@link ElasticsearchClient} is mocked.
 */
class JavaClientSearchServiceTest {

    // Build REAL co.elastic client objects (Hit/HitsMetadata/SearchResponse) rather than mocking
    // them: their accessors are final methods that the mock maker cannot stub.
    private static SearchResponse<Product> responseWith(List<Hit<Product>> hits) {
        return SearchResponse.of(r -> r
                .took(1L)
                .timedOut(false)
                .shards(s -> s.total(1).successful(1).failed(0))
                .hits(h -> h
                        .total(t -> t.value(hits.size()).relation(co.elastic.clients.elasticsearch.core.search.TotalHitsRelation.Eq))
                        .hits(hits)));
    }

    private static Hit<Product> hit(String id, Double score, Product source) {
        return Hit.of(h -> h.index("search-products").id(id).score(score).source(source));
    }

    @Test
    @SuppressWarnings("unchecked")
    void mapsHitsToResponse() throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        Product p = new Product("p01", "Apple MacBook Pro", "laptop", "Apple",
                "pro laptop", new BigDecimal("1999.00"), 12);
        when(client.search(any(Function.class), any(Class.class)))
                .thenReturn(responseWith(List.of(hit("p01", 1.5, p))));

        StepVerifier.create(new JavaClientSearchService(client).term("category", "laptop"))
                .assertNext(resp -> {
                    assertEquals("java-client", resp.client());
                    assertEquals(1, resp.total());
                    assertEquals("p01", resp.hits().get(0).id());
                    assertEquals("laptop", resp.hits().get(0).category());
                    assertEquals(1.5f, resp.hits().get(0).score());
                })
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void emptyResultYieldsEmptyList() throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        when(client.search(any(Function.class), any(Class.class)))
                .thenReturn(responseWith(List.of()));

        StepVerifier.create(new JavaClientSearchService(client).match("name", "zzz"))
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
        StepVerifier.create(new JavaClientSearchService(client).term("brand", "Apple"))
                .expectNextCount(1)
                .verifyComplete();

        String actual = thread.get();
        assertNotNull(actual, "search() was never invoked");
        assertTrue(actual.startsWith("boundedElastic-"),
                "blocking search must run on a boundedElastic worker but ran on: " + actual);
        assertNotEquals(caller, actual, "blocking search must not run on the caller thread");
    }
}
