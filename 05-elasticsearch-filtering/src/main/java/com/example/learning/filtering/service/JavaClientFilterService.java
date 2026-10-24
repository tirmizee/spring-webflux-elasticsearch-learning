package com.example.learning.filtering.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.learning.filtering.domain.Product;
import com.example.learning.filtering.dto.FilterResponse;
import com.example.learning.filtering.dto.FilterSearchRequest;
import com.example.learning.filtering.dto.ProductHit;
import com.example.learning.filtering.dto.RangeFilter;
import com.example.learning.filtering.query.ProductFilters;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.List;

/**
 * Filtering via the official Elasticsearch Java API Client ({@link ElasticsearchClient}).
 *
 * <p>The client is <strong>synchronous</strong>; each search is wrapped in {@link Mono#fromCallable}
 * and moved onto {@link Schedulers#boundedElastic()} so the blocking HTTP call never runs on the
 * reactor-netty event loop. Wrapping a blocking call in a {@code Mono} does <em>not</em> make it
 * non-blocking — the work is still blocking, it is just offloaded to a worker thread. The query is
 * the same {@code co.elastic.clients} {@link Query} built by {@link ProductFilters} that the Spring
 * Data service uses, so results are directly comparable.
 */
@Service
public class JavaClientFilterService {

    private static final String CLIENT = "java-client";
    private static final String INDEX = "filter-products";

    private final ElasticsearchClient client;

    public JavaClientFilterService(ElasticsearchClient client) {
        this.client = client;
    }

    public Mono<FilterResponse> term(String field, String value) {
        return execute(ProductFilters.filterContext(List.of(ProductFilters.term(field, value))));
    }

    public Mono<FilterResponse> termBoolean(String field, boolean value) {
        return execute(ProductFilters.filterContext(List.of(ProductFilters.term(field, value))));
    }

    public Mono<FilterResponse> terms(String field, List<String> values) {
        return execute(ProductFilters.filterContext(List.of(ProductFilters.terms(field, values))));
    }

    public Mono<FilterResponse> range(RangeFilter filter) {
        return execute(ProductFilters.filterContext(List.of(ProductFilters.range(filter))));
    }

    public Mono<FilterResponse> exists(String field) {
        return execute(ProductFilters.filterContext(List.of(ProductFilters.exists(field))));
    }

    public Mono<FilterResponse> compose(FilterSearchRequest request) {
        return execute(ProductFilters.compose(request));
    }

    private Mono<FilterResponse> execute(Query query) {
        return Mono.fromCallable(() -> blockingSearch(query))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private FilterResponse blockingSearch(Query query) throws IOException {
        co.elastic.clients.elasticsearch.core.SearchResponse<Product> response =
                client.search(s -> s.index(INDEX).query(query), Product.class);
        List<ProductHit> hits = response.hits().hits().stream()
                .map(this::toHit)
                .toList();
        return FilterResponse.of(CLIENT, hits);
    }

    private ProductHit toHit(Hit<Product> hit) {
        // In pure filter context the Java API Client returns a null score; surface it as 0.0 so
        // the response is deterministic and matches the Spring Data path.
        Float score = hit.score() == null ? 0.0f : hit.score().floatValue();
        return ProductHit.of(hit.id(), score, hit.source());
    }
}
