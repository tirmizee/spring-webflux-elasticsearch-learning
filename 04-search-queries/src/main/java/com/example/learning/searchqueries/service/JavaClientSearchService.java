package com.example.learning.searchqueries.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.learning.searchqueries.domain.Product;
import com.example.learning.searchqueries.dto.BoolSearchRequest;
import com.example.learning.searchqueries.dto.ProductHit;
import com.example.learning.searchqueries.dto.SearchResponse;
import com.example.learning.searchqueries.query.ProductQueries;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.List;

/**
 * Search via the official Elasticsearch Java API Client ({@link ElasticsearchClient}).
 *
 * <p>The client is <strong>synchronous</strong>; each search is wrapped in {@link Mono#fromCallable}
 * and moved onto {@link Schedulers#boundedElastic()} so the blocking HTTP call never runs on the
 * reactor-netty event loop. The query is the same {@code co.elastic.clients} {@link Query} built by
 * {@link ProductQueries} that the Spring Data service uses, so results are directly comparable.
 */
@Service
public class JavaClientSearchService {

    private static final String CLIENT = "java-client";
    private static final String INDEX = "search-products";

    private final ElasticsearchClient client;

    public JavaClientSearchService(ElasticsearchClient client) {
        this.client = client;
    }

    public Mono<SearchResponse> term(String field, String value) {
        return execute(ProductQueries.term(field, value));
    }

    public Mono<SearchResponse> match(String field, String text) {
        return execute(ProductQueries.match(field, text));
    }

    public Mono<SearchResponse> bool(BoolSearchRequest request) {
        return execute(ProductQueries.bool(request));
    }

    private Mono<SearchResponse> execute(Query query) {
        return Mono.fromCallable(() -> blockingSearch(query))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private SearchResponse blockingSearch(Query query) throws IOException {
        co.elastic.clients.elasticsearch.core.SearchResponse<Product> response =
                client.search(s -> s.index(INDEX).query(query), Product.class);
        List<ProductHit> hits = response.hits().hits().stream()
                .map(this::toHit)
                .toList();
        return SearchResponse.of(CLIENT, hits);
    }

    private ProductHit toHit(Hit<Product> hit) {
        Float score = hit.score() == null ? null : hit.score().floatValue();
        return ProductHit.of(hit.id(), score, hit.source());
    }
}
