package com.example.learning.searchqueries.controller;

import com.example.learning.searchqueries.dto.ProductHit;
import com.example.learning.searchqueries.dto.SearchResponse;
import com.example.learning.searchqueries.exception.GlobalExceptionHandler;
import com.example.learning.searchqueries.service.JavaClientSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Mirrors the Spring Data controller test to confirm both clients expose identical behavior. */
@WebFluxTest(controllers = JavaClientSearchController.class)
@Import(GlobalExceptionHandler.class)
class JavaClientSearchControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private JavaClientSearchService service;

    private static ProductHit hit(String id) {
        return new ProductHit(id, "Dell XPS Laptop", "laptop", "Dell",
                "ultrabook", new BigDecimal("1499.00"), 8, 2.0f);
    }

    @Test
    void termReturns200() {
        when(service.term("brand", "Dell"))
                .thenReturn(Mono.just(SearchResponse.of("java-client", List.of(hit("p03")))));

        webTestClient.get().uri("/api/java-client/products/search/term?field=brand&value=Dell")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.client").isEqualTo("java-client")
                .jsonPath("$.hits[0].brand").isEqualTo("Dell");
    }

    @Test
    void emptyResultReturns200() {
        when(service.match("description", "zzz"))
                .thenReturn(Mono.just(SearchResponse.of("java-client", List.of())));

        webTestClient.get().uri("/api/java-client/products/search/match?field=description&text=zzz")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.hits.length()").isEqualTo(0);
    }

    @Test
    void boolReturns200() {
        when(service.bool(any()))
                .thenReturn(Mono.just(SearchResponse.of("java-client", List.of(hit("p03")))));

        String body = """
                {"must":[{"type":"term","field":"category","value":"laptop"}],
                 "mustNot":[{"type":"term","field":"brand","value":"Apple"}]}
                """;
        webTestClient.post().uri("/api/java-client/products/search/bool")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.hits[0].id").isEqualTo("p03");
    }

    @Test
    void boolMissingFieldReturns400() {
        String body = """
                {"must":[{"type":"term","value":"laptop"}]}
                """;
        webTestClient.post().uri("/api/java-client/products/search/bool")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
