package com.example.learning.basicconnection.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Verifies that {@link GlobalExceptionHandler} classifies errors into the correct HTTP status and
 * that the response body never leaks infrastructure details (host, URI, stack trace, raw message).
 *
 * <p>{@link ErrorThrowingTestController} is mounted inside the WebFlux slice to raise each kind of
 * exception on demand.
 */
@WebFluxTest(controllers = ErrorThrowingTestController.class)
@Import({ErrorThrowingTestController.class, GlobalExceptionHandler.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void connectExceptionMapsTo503() {
        webTestClient.get().uri("/throw/connect")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Elasticsearch unavailable")
                .jsonPath("$.status").isEqualTo(503);
    }

    @Test
    void ioExceptionMapsTo503AndDoesNotLeakDetails() {
        webTestClient.get().uri("/throw/io")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                // the raw message mentioned an internal host:port — it must NOT appear in the body
                .jsonPath("$.detail").value(d -> {
                    String detail = (String) d;
                    if (detail.contains("es-node-1") || detail.contains("9200")) {
                        throw new AssertionError("error body leaked infrastructure details: " + detail);
                    }
                });
    }

    @Test
    void elasticsearchResponseErrorMapsTo502() {
        webTestClient.get().uri("/throw/es")
                .exchange()
                .expectStatus().isEqualTo(502)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Elasticsearch returned an error")
                .jsonPath("$.status").isEqualTo(502)
                // must not echo the raw ES reason/type
                .jsonPath("$.detail").value(d -> {
                    String detail = (String) d;
                    if (detail.contains("all shards failed") || detail.contains("search_phase")) {
                        throw new AssertionError("error body leaked raw ES message: " + detail);
                    }
                });
    }

    @Test
    void wrappedConnectExceptionStillMapsTo503() {
        webTestClient.get().uri("/throw/wrapped-connect")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Elasticsearch unavailable");
    }

    @Test
    void unexpectedApplicationErrorMapsTo500() {
        webTestClient.get().uri("/throw/bug")
                .exchange()
                .expectStatus().isEqualTo(500)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Internal server error")
                .jsonPath("$.status").isEqualTo(500)
                .jsonPath("$.detail").value(d -> {
                    String detail = (String) d;
                    if (detail.contains("null pointer") || detail.contains("line 42")) {
                        throw new AssertionError("error body leaked internal error detail: " + detail);
                    }
                });
    }
}
