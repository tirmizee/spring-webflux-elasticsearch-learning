package com.example.learning.documentcrud.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.example.learning.documentcrud.domain.Product;
import com.example.learning.documentcrud.dto.ProductRequest;
import com.example.learning.documentcrud.dto.ProductResponse;
import com.example.learning.documentcrud.exception.DuplicateProductException;
import com.example.learning.documentcrud.exception.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Java API Client product service: duplicate → 409, missing → 404, mapping,
 * and blocking-scheduler isolation. The {@link ElasticsearchClient} is mocked.
 */
class JavaClientProductServiceTest {

    private final ProductRequest request =
            new ProductRequest("Keyboard", "mechanical", new BigDecimal("129.90"), 10);

    @Test
    @SuppressWarnings("unchecked")
    void createDuplicateMapsToDuplicateException() throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        ErrorResponse err = ErrorResponse.of(r -> r.status(409)
                .error(e -> e.type("version_conflict_engine_exception").reason("document already exists")));
        when(client.index(any(Function.class))).thenThrow(new ElasticsearchException("index", err));

        StepVerifier.create(new JavaClientProductService(client).create("p1", request))
                .expectError(DuplicateProductException.class)
                .verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getMissingMapsToNotFound() throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        GetResponse<Product> notFound = mock(GetResponse.class);
        when(notFound.found()).thenReturn(false);
        when(client.get(any(Function.class), any(Class.class))).thenReturn(notFound);

        StepVerifier.create(new JavaClientProductService(client).get("nope"))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getExistingMapsSource() throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        GetResponse<Product> found = mock(GetResponse.class);
        when(found.found()).thenReturn(true);
        when(found.source()).thenReturn(new Product("p1", "Keyboard", "mechanical", new BigDecimal("129.90"), 10));
        when(client.get(any(Function.class), any(Class.class))).thenReturn(found);

        StepVerifier.create(new JavaClientProductService(client).get("p1"))
                .expectNext(new ProductResponse("java-client", "p1", "Keyboard", "mechanical", new BigDecimal("129.90"), 10))
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateMissingMapsToNotFound() throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        when(client.exists(any(Function.class))).thenReturn(new BooleanResponse(false));

        StepVerifier.create(new JavaClientProductService(client).update("nope", request))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteMissingMapsToNotFound() throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        when(client.exists(any(Function.class))).thenReturn(new BooleanResponse(false));

        StepVerifier.create(new JavaClientProductService(client).delete("nope"))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void createRunsBlockingCallOnBoundedElastic() throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        AtomicReference<String> thread = new AtomicReference<>();
        when(client.index(any(Function.class))).thenAnswer(inv -> {
            thread.set(Thread.currentThread().getName());
            return mock(IndexResponse.class);
        });

        String caller = Thread.currentThread().getName();
        StepVerifier.create(new JavaClientProductService(client).create("p1", request))
                .expectNextCount(1)
                .verifyComplete();

        String actual = thread.get();
        assertNotNull(actual, "index() was never invoked");
        assertTrue(actual.startsWith("boundedElastic-"),
                "blocking call must run on a boundedElastic worker but ran on: " + actual);
        assertNotEquals(caller, actual, "blocking call must not run on the caller thread");
    }
}
