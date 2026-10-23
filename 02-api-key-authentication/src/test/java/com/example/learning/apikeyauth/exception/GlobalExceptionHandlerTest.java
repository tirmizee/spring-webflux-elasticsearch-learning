package com.example.learning.apikeyauth.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Verifies that {@link GlobalExceptionHandler} classifies errors into the correct HTTP status,
 * clearly distinguishing authentication (401) from authorization (403), and that response bodies
 * never leak credentials, API keys, hosts, or raw Elasticsearch messages.
 */
@WebFluxTest(controllers = ErrorThrowingTestController.class)
@Import({ErrorThrowingTestController.class, GlobalExceptionHandler.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void missingOrInvalidApiKeyMapsTo401() {
        webTestClient.get().uri("/throw/unauthorized")
                .exchange()
                .expectStatus().isEqualTo(401)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Elasticsearch authentication failed")
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.detail").value(GlobalExceptionHandlerTest::assertNoLeak);
    }

    @Test
    void insufficientPrivilegesMapsTo403() {
        webTestClient.get().uri("/throw/forbidden")
                .exchange()
                .expectStatus().isEqualTo(403)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Elasticsearch authorization failed")
                .jsonPath("$.status").isEqualTo(403)
                // must not echo the raw ES reason (which named a key id and an action)
                .jsonPath("$.detail").value(GlobalExceptionHandlerTest::assertNoLeak);
    }

    @Test
    void wrappedUnauthorizedStillMapsTo401() {
        webTestClient.get().uri("/throw/wrapped-unauthorized")
                .exchange()
                .expectStatus().isEqualTo(401)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Elasticsearch authentication failed")
                .jsonPath("$.detail").value(GlobalExceptionHandlerTest::assertNoLeak);
    }

    @Test
    void otherElasticsearchErrorMapsTo502() {
        webTestClient.get().uri("/throw/es-other")
                .exchange()
                .expectStatus().isEqualTo(502)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Elasticsearch returned an error");
    }

    @Test
    void connectExceptionMapsTo503() {
        webTestClient.get().uri("/throw/connect")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Elasticsearch unavailable")
                .jsonPath("$.detail").value(GlobalExceptionHandlerTest::assertNoLeak);
    }

    @Test
    void timeoutMapsTo504() {
        webTestClient.get().uri("/throw/timeout")
                .exchange()
                .expectStatus().isEqualTo(504)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Elasticsearch timeout")
                .jsonPath("$.detail").value(GlobalExceptionHandlerTest::assertNoLeak);
    }

    @Test
    void ioExceptionMapsTo503() {
        webTestClient.get().uri("/throw/io")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.detail").value(GlobalExceptionHandlerTest::assertNoLeak);
    }

    @Test
    void unexpectedErrorMapsTo500() {
        webTestClient.get().uri("/throw/bug")
                .exchange()
                .expectStatus().isEqualTo(500)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Internal server error")
                .jsonPath("$.detail").value(d -> {
                    String detail = (String) d;
                    if (detail.contains("null pointer") || detail.contains("line 42")) {
                        throw new AssertionError("error body leaked internal detail: " + detail);
                    }
                });
    }

    /** Asserts the response detail does not leak host/port, key ids, or raw ES security messages. */
    private static void assertNoLeak(Object detail) {
        String s = (String) detail;
        String[] forbidden = {"9200", "es-node-1", "127.0.0.1", "ApiKey", "api key id", "abc123", "xyz",
                "credentials", "cluster:monitor"};
        for (String f : forbidden) {
            if (s.contains(f)) {
                throw new AssertionError("error body leaked sensitive detail ('" + f + "'): " + s);
            }
        }
    }
}
