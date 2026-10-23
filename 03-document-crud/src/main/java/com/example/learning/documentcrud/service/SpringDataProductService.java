package com.example.learning.documentcrud.service;

import com.example.learning.documentcrud.domain.Product;
import com.example.learning.documentcrud.dto.ProductRequest;
import com.example.learning.documentcrud.dto.ProductResponse;
import com.example.learning.documentcrud.exception.DuplicateProductException;
import com.example.learning.documentcrud.exception.ProductNotFoundException;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Product CRUD via Spring Data Elasticsearch reactive ({@link ReactiveElasticsearchOperations}).
 *
 * <p>Calls are genuinely non-blocking. Writes use {@link RefreshPolicy#IMMEDIATE} so a subsequent
 * real-time GET (and tests) observe the change at once; this makes the learning examples
 * deterministic. The README explains Elasticsearch's near-real-time search vs. real-time GET.
 *
 * <p>CRUD semantics are explicit (Spring Data {@code save} is an upsert, so we gate on existence):
 * create fails if the id already exists (409), update fails if it does not exist (404), and we
 * never silently turn an update into an insert.
 */
@Service
public class SpringDataProductService {

    private static final String CLIENT = "spring-data";

    private final ReactiveElasticsearchOperations operations;

    public SpringDataProductService(ReactiveElasticsearchOperations operations) {
        // IMMEDIATE refresh for predictable read-after-write in a learning context.
        this.operations = operations.withRefreshPolicy(RefreshPolicy.IMMEDIATE);
    }

    /** Create with an explicit id; 409 if the id already exists (no upsert). */
    public Mono<ProductResponse> create(String id, ProductRequest request) {
        return operations.exists(id, Product.class)
                .flatMap(exists -> exists
                        ? Mono.error(new DuplicateProductException(id))
                        : operations.save(toProduct(id, request)))
                .map(saved -> ProductResponse.of(CLIENT, saved));
    }

    /** Real-time GET by id; 404 if missing. */
    public Mono<ProductResponse> get(String id) {
        return operations.get(id, Product.class)
                .map(p -> ProductResponse.of(CLIENT, p))
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)));
    }

    /** Full replacement of an existing document; 404 if it does not exist (no upsert). */
    public Mono<ProductResponse> update(String id, ProductRequest request) {
        return operations.exists(id, Product.class)
                .flatMap(exists -> exists
                        ? operations.save(toProduct(id, request))
                        : Mono.error(new ProductNotFoundException(id)))
                .map(saved -> ProductResponse.of(CLIENT, saved));
    }

    /** Delete by id; 404 if missing. */
    public Mono<Void> delete(String id) {
        return operations.exists(id, Product.class)
                .flatMap(exists -> exists
                        ? operations.delete(id, Product.class).then()
                        : Mono.error(new ProductNotFoundException(id)));
    }

    private Product toProduct(String id, ProductRequest r) {
        return new Product(id, r.name(), r.description(), r.price(), r.quantity());
    }
}
