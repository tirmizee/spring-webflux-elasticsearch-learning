package com.example.learning.filtering.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.learning.filtering.domain.Product;
import com.example.learning.filtering.dto.FilterResponse;
import com.example.learning.filtering.dto.FilterSearchRequest;
import com.example.learning.filtering.dto.ProductHit;
import com.example.learning.filtering.dto.RangeFilter;
import com.example.learning.filtering.query.ProductFilters;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Filtering via Spring Data Elasticsearch reactive ({@link ReactiveElasticsearchOperations}).
 *
 * <p>Genuinely non-blocking: {@code search(...)} returns a {@code Flux<SearchHit<Product>>} which we
 * map to the stable response DTO. Every query is built by {@link ProductFilters} from
 * {@code co.elastic.clients} builders and wrapped in a {@link NativeQuery}, so it is identical to
 * the query the Java API Client runs.
 */
@Service
public class SpringDataFilterService {

    private static final String CLIENT = "spring-data";

    private final ReactiveElasticsearchOperations operations;

    public SpringDataFilterService(ReactiveElasticsearchOperations operations) {
        this.operations = operations;
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
        NativeQuery nativeQuery = NativeQuery.builder().withQuery(query).build();
        return operations.search(nativeQuery, Product.class)
                .map(this::toHit)
                .collectList()
                .map(hits -> FilterResponse.of(CLIENT, hits));
    }

    private ProductHit toHit(SearchHit<Product> hit) {
        return ProductHit.of(hit.getId(), normalizeScore(hit.getScore()), hit.getContent());
    }

    /**
     * In pure filter context Elasticsearch returns no relevance score; Spring Data surfaces that as
     * {@code NaN}. Normalize to {@code 0.0} so filter-context responses are deterministic and
     * clearly show "no scoring". Query-context matches keep their positive score.
     */
    private Float normalizeScore(float score) {
        return Float.isNaN(score) ? 0.0f : score;
    }
}
