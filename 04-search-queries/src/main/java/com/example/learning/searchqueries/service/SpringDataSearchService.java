package com.example.learning.searchqueries.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.learning.searchqueries.domain.Product;
import com.example.learning.searchqueries.dto.BoolSearchRequest;
import com.example.learning.searchqueries.dto.ProductHit;
import com.example.learning.searchqueries.dto.SearchResponse;
import com.example.learning.searchqueries.query.ProductQueries;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Search via Spring Data Elasticsearch reactive ({@link ReactiveElasticsearchOperations}).
 *
 * <p>Genuinely non-blocking: {@code search(...)} returns a {@code Flux<SearchHit<Product>>} which we
 * map to the stable response DTO. The query itself is built by {@link ProductQueries} from
 * {@code co.elastic.clients} builders and wrapped in a {@link NativeQuery}, so it is identical to
 * the query the Java API Client runs.
 */
@Service
public class SpringDataSearchService {

    private static final String CLIENT = "spring-data";

    private final ReactiveElasticsearchOperations operations;

    public SpringDataSearchService(ReactiveElasticsearchOperations operations) {
        this.operations = operations;
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
        NativeQuery nativeQuery = NativeQuery.builder().withQuery(query).build();
        return operations.search(nativeQuery, Product.class)
                .map(this::toHit)
                .collectList()
                .map(hits -> SearchResponse.of(CLIENT, hits));
    }

    private ProductHit toHit(SearchHit<Product> hit) {
        return ProductHit.of(hit.getId(), hit.getScore(), hit.getContent());
    }
}
