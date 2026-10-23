package com.example.learning.documentcrud.service;

import com.example.learning.documentcrud.domain.Product;
import com.example.learning.documentcrud.dto.ProductRequest;
import com.example.learning.documentcrud.dto.ProductResponse;
import com.example.learning.documentcrud.exception.DuplicateProductException;
import com.example.learning.documentcrud.exception.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Spring Data product service: create/dup/get/update/delete semantics with a
 * mocked {@link ReactiveElasticsearchOperations}. Verifies no silent upsert (create fails on
 * existing id, update fails on missing id) and 404/409 propagation.
 */
class SpringDataProductServiceTest {

    private ReactiveElasticsearchOperations operations;
    private SpringDataProductService service;

    private final ProductRequest request =
            new ProductRequest("Keyboard", "mechanical", new BigDecimal("129.90"), 10);

    @BeforeEach
    void setUp() {
        operations = mock(ReactiveElasticsearchOperations.class);
        // The service calls withRefreshPolicy(IMMEDIATE) in its constructor.
        when(operations.withRefreshPolicy(RefreshPolicy.IMMEDIATE)).thenReturn(operations);
        service = new SpringDataProductService(operations);
    }

    @Test
    void createSavesWhenIdFree() {
        when(operations.exists(eq("p1"), eq(Product.class))).thenReturn(Mono.just(false));
        when(operations.save(any(Product.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.create("p1", request))
                .expectNext(new ProductResponse("spring-data", "p1", "Keyboard", "mechanical", new BigDecimal("129.90"), 10))
                .verifyComplete();
    }

    @Test
    void createFailsWhenIdExists() {
        when(operations.exists(eq("p1"), eq(Product.class))).thenReturn(Mono.just(true));

        StepVerifier.create(service.create("p1", request))
                .expectError(DuplicateProductException.class)
                .verify();
    }

    @Test
    void getMissingFailsWithNotFound() {
        when(operations.get(eq("nope"), eq(Product.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.get("nope"))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    void updateFailsWhenMissing() {
        when(operations.exists(eq("nope"), eq(Product.class))).thenReturn(Mono.just(false));

        StepVerifier.create(service.update("nope", request))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    void updateSavesWhenExists() {
        when(operations.exists(eq("p1"), eq(Product.class))).thenReturn(Mono.just(true));
        when(operations.save(any(Product.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.update("p1", request))
                .expectNextMatches(r -> r.id().equals("p1") && r.name().equals("Keyboard"))
                .verifyComplete();
    }

    @Test
    void deleteMissingFailsWithNotFound() {
        when(operations.exists(eq("nope"), eq(Product.class))).thenReturn(Mono.just(false));

        StepVerifier.create(service.delete("nope"))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    void deleteExistingCompletes() {
        when(operations.exists(eq("p1"), eq(Product.class))).thenReturn(Mono.just(true));
        when(operations.delete(eq("p1"), eq(Product.class))).thenReturn(Mono.just("p1"));

        StepVerifier.create(service.delete("p1"))
                .verifyComplete();
    }
}
