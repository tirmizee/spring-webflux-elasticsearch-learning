package com.example.learning.basicconnection.controller;

import com.example.learning.basicconnection.dto.ClusterHealthResponse;
import com.example.learning.basicconnection.dto.ClusterInfoResponse;
import com.example.learning.basicconnection.exception.GlobalExceptionHandler;
import com.example.learning.basicconnection.service.SpringDataConnectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.net.ConnectException;

import static org.mockito.Mockito.when;

/**
 * Controller slice test for the Spring Data approach. The service is mocked, so this
 * verifies routing, status codes and JSON serialization without touching Elasticsearch.
 */
@WebFluxTest(controllers = SpringDataConnectionController.class)
@Import(GlobalExceptionHandler.class)
class SpringDataConnectionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private SpringDataConnectionService service;

    @Test
    void healthReturnsMappedBody() {
        when(service.clusterHealth()).thenReturn(Mono.just(
                new ClusterHealthResponse("spring-data", "test-cluster", "green", 1, true)));

        webTestClient.get().uri("/api/spring-data/elasticsearch/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.client").isEqualTo("spring-data")
                .jsonPath("$.clusterName").isEqualTo("test-cluster")
                .jsonPath("$.status").isEqualTo("green")
                .jsonPath("$.numberOfNodes").isEqualTo(1)
                .jsonPath("$.reachable").isEqualTo(true);
    }

    @Test
    void clusterInfoReturnsMappedBody() {
        when(service.clusterInfo()).thenReturn(Mono.just(
                new ClusterInfoResponse("spring-data", "test-cluster", "uuid-123", "9.4.8", "You Know, for Search")));

        webTestClient.get().uri("/api/spring-data/elasticsearch/cluster-info")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.client").isEqualTo("spring-data")
                .jsonPath("$.clusterName").isEqualTo("test-cluster")
                .jsonPath("$.versionNumber").isEqualTo("9.4.8")
                .jsonPath("$.tagline").isEqualTo("You Know, for Search");
    }

    @Test
    void whenElasticsearchUnreachableThenServiceUnavailable() {
        // A connectivity failure (ES down) must be classified as 503 by GlobalExceptionHandler.
        when(service.clusterHealth()).thenReturn(Mono.error(new ConnectException("connection refused")));

        webTestClient.get().uri("/api/spring-data/elasticsearch/health")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Elasticsearch unavailable");
    }
}
