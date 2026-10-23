package com.example.learning.searchqueries.controller;

import com.example.learning.searchqueries.dto.ProductHit;
import com.example.learning.searchqueries.dto.SearchResponse;
import com.example.learning.searchqueries.exception.GlobalExceptionHandler;
import com.example.learning.searchqueries.service.SpringDataSearchService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = SpringDataSearchController.class)
@Import(GlobalExceptionHandler.class)
class SpringDataSearchControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private SpringDataSearchService service;

    private static ProductHit hit(String id) {
        return new ProductHit(id, "Apple MacBook Pro", "laptop", "Apple",
                "pro laptop", new BigDecimal("1999.00"), 12, 1.5f);
    }

    @Test
    void termReturns200WithHits() {
        when(service.term("category", "laptop"))
                .thenReturn(Mono.just(SearchResponse.of("spring-data", List.of(hit("p01")))));

        webTestClient.get().uri("/api/spring-data/products/search/term?field=category&value=laptop")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.client").isEqualTo("spring-data")
                .jsonPath("$.total").isEqualTo(1)
                .jsonPath("$.hits[0].id").isEqualTo("p01")
                .jsonPath("$.hits[0].category").isEqualTo("laptop");
    }

    @Test
    void matchReturns200() {
        when(service.match("name", "macbook"))
                .thenReturn(Mono.just(SearchResponse.of("spring-data", List.of(hit("p01")))));

        webTestClient.get().uri("/api/spring-data/products/search/match?field=name&text=macbook")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.hits[0].score").isEqualTo(1.5);
    }

    @Test
    void emptyResultReturns200WithEmptyList() {
        when(service.term(eq("brand"), eq("Nonexistent")))
                .thenReturn(Mono.just(SearchResponse.of("spring-data", List.of())));

        webTestClient.get().uri("/api/spring-data/products/search/term?field=brand&value=Nonexistent")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.total").isEqualTo(0)
                .jsonPath("$.hits").isArray()
                .jsonPath("$.hits.length()").isEqualTo(0);
    }

    @Test
    void boolReturns200() {
        when(service.bool(any()))
                .thenReturn(Mono.just(SearchResponse.of("spring-data", List.of(hit("p01")))));

        String body = """
                {"must":[{"type":"term","field":"category","value":"laptop"}],
                 "should":[{"type":"match","field":"name","value":"pro"}]}
                """;
        webTestClient.post().uri("/api/spring-data/products/search/bool")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.total").isEqualTo(1);
    }

    @Test
    void boolInvalidClauseTypeReturns400() {
        String body = """
                {"must":[{"type":"wildcard","field":"category","value":"laptop"}]}
                """;
        webTestClient.post().uri("/api/spring-data/products/search/bool")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.title").isEqualTo("Invalid request");
    }

    // ---- Finding #2: minimumShouldMatch validation ----

    @Test
    void boolNegativeMinimumShouldMatchReturns400AndSkipsService() {
        String body = """
                {"must":[{"type":"term","field":"category","value":"laptop"}],"minimumShouldMatch":-1}
                """;
        webTestClient.post().uri("/api/spring-data/products/search/bool")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.title").isEqualTo("Invalid request");
        verifyNoInteractions(service);
    }

    @Test
    void boolZeroMinimumShouldMatchIsAccepted() {
        when(service.bool(any())).thenReturn(Mono.just(SearchResponse.of("spring-data", List.of(hit("p01")))));
        String body = """
                {"must":[{"type":"term","field":"category","value":"laptop"}],"minimumShouldMatch":0}
                """;
        webTestClient.post().uri("/api/spring-data/products/search/bool")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void boolPositiveMinimumShouldMatchIsAccepted() {
        when(service.bool(any())).thenReturn(Mono.just(SearchResponse.of("spring-data", List.of(hit("p01")))));
        String body = """
                {"must":[{"type":"term","field":"category","value":"laptop"}],
                 "should":[{"type":"match","field":"name","value":"pro"}],"minimumShouldMatch":1}
                """;
        webTestClient.post().uri("/api/spring-data/products/search/bool")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange()
                .expectStatus().isOk();
    }

    // ---- Finding #3: GET parameter validation ----

    @Test
    void termWhitespaceFieldReturns400AndSkipsService() {
        webTestClient.get()
                .uri(b -> b.path("/api/spring-data/products/search/term")
                        .queryParam("field", " ").queryParam("value", "laptop").build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.title").isEqualTo("Invalid request");
        verifyNoInteractions(service);
    }

    @Test
    void termEmptyValueReturns400() {
        webTestClient.get().uri("/api/spring-data/products/search/term?field=category&value=")
                .exchange()
                .expectStatus().isBadRequest();
        verifyNoInteractions(service);
    }

    @Test
    void termMissingFieldReturns400() {
        webTestClient.get().uri("/api/spring-data/products/search/term?value=laptop")
                .exchange()
                .expectStatus().isBadRequest();
        verifyNoInteractions(service);
    }

    @Test
    void matchWhitespaceTextReturns400() {
        webTestClient.get()
                .uri(b -> b.path("/api/spring-data/products/search/match")
                        .queryParam("field", "name").queryParam("text", "  ").build())
                .exchange()
                .expectStatus().isBadRequest();
        verifyNoInteractions(service);
    }
}
