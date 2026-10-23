package com.example.learning.basicconnection.javaclient;

import com.example.learning.basicconnection.dto.ClusterHealthResponse;
import com.example.learning.basicconnection.dto.ClusterInfoResponse;
import com.example.learning.basicconnection.web.GlobalErrorHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;

/**
 * Controller slice test for the Java API Client approach. The service is mocked.
 */
@WebFluxTest(controllers = JavaClientElasticsearchController.class)
@Import(GlobalErrorHandler.class)
class JavaClientElasticsearchControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private JavaClientElasticsearchService service;

    @Test
    void healthReturnsMappedBody() {
        when(service.clusterHealth()).thenReturn(Mono.just(
                new ClusterHealthResponse("java-client", "test-cluster", "yellow", 1, true)));

        webTestClient.get().uri("/api/java-client/elasticsearch/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.client").isEqualTo("java-client")
                .jsonPath("$.status").isEqualTo("yellow")
                .jsonPath("$.reachable").isEqualTo(true);
    }

    @Test
    void clusterInfoReturnsMappedBody() {
        when(service.clusterInfo()).thenReturn(Mono.just(
                new ClusterInfoResponse("java-client", "test-cluster", "uuid-123", "9.4.8", "You Know, for Search")));

        webTestClient.get().uri("/api/java-client/elasticsearch/cluster-info")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.client").isEqualTo("java-client")
                .jsonPath("$.versionNumber").isEqualTo("9.4.8");
    }
}
