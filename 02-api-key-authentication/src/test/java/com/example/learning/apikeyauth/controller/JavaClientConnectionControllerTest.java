package com.example.learning.apikeyauth.controller;

import com.example.learning.apikeyauth.dto.ClusterHealthResponse;
import com.example.learning.apikeyauth.dto.ClusterInfoResponse;
import com.example.learning.apikeyauth.exception.GlobalExceptionHandler;
import com.example.learning.apikeyauth.service.JavaClientConnectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;

@WebFluxTest(controllers = JavaClientConnectionController.class)
@Import(GlobalExceptionHandler.class)
class JavaClientConnectionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private JavaClientConnectionService service;

    @Test
    void healthReturnsMappedBody() {
        when(service.clusterHealth()).thenReturn(Mono.just(
                new ClusterHealthResponse("java-client", "secure-cluster", "green", 1, true)));

        webTestClient.get().uri("/api/java-client/elasticsearch/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.client").isEqualTo("java-client")
                .jsonPath("$.reachable").isEqualTo(true);
    }

    @Test
    void clusterInfoReturnsMappedBody() {
        when(service.clusterInfo()).thenReturn(Mono.just(
                new ClusterInfoResponse("java-client", "secure-cluster", "uuid-1", "9.4.8", "You Know, for Search")));

        webTestClient.get().uri("/api/java-client/elasticsearch/cluster-info")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.client").isEqualTo("java-client")
                .jsonPath("$.versionNumber").isEqualTo("9.4.8");
    }
}
