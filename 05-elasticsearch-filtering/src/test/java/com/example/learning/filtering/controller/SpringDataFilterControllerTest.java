package com.example.learning.filtering.controller;

import com.example.learning.filtering.dto.FilterResponse;
import com.example.learning.filtering.dto.ProductHit;
import com.example.learning.filtering.dto.RangeFilter;
import com.example.learning.filtering.exception.GlobalExceptionHandler;
import com.example.learning.filtering.service.SpringDataFilterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = SpringDataFilterController.class)
@Import(GlobalExceptionHandler.class)
class SpringDataFilterControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private SpringDataFilterService service;

    private static ProductHit hit(String id) {
        return new ProductHit(id, "Apple MacBook Pro", "laptop", "Apple",
                new BigDecimal("1999.00"), 4.8, LocalDate.parse("2024-03-10"), true, 10, 0.0f);
    }

    @Test
    void termReturns200WithHits() {
        when(service.term("category", "laptop"))
                .thenReturn(Mono.just(FilterResponse.of("spring-data", List.of(hit("p01")))));

        webTestClient.get().uri("/api/spring-data/products/filter/term?field=category&value=laptop")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.client").isEqualTo("spring-data")
                .jsonPath("$.total").isEqualTo(1)
                .jsonPath("$.hits[0].id").isEqualTo("p01")
                .jsonPath("$.hits[0].score").isEqualTo(0.0);
    }

    @Test
    void termBooleanReturns200() {
        when(service.termBoolean("inStock", true))
                .thenReturn(Mono.just(FilterResponse.of("spring-data", List.of(hit("p01")))));

        webTestClient.get().uri("/api/spring-data/products/filter/term/boolean?field=inStock&value=true")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.total").isEqualTo(1);
    }

    @Test
    void termsReturns200() {
        when(service.terms(eq("brand"), anyList()))
                .thenReturn(Mono.just(FilterResponse.of("spring-data", List.of(hit("p01")))));

        webTestClient.get().uri("/api/spring-data/products/filter/terms?field=brand&values=Apple,Dell")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.total").isEqualTo(1);
    }

    @Test
    void rangeReturns200() {
        when(service.range(any(RangeFilter.class)))
                .thenReturn(Mono.just(FilterResponse.of("spring-data", List.of(hit("p01")))));

        webTestClient.get().uri("/api/spring-data/products/filter/range?field=price&type=number&gte=1000")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.total").isEqualTo(1);
    }

    @Test
    void existsReturns200() {
        when(service.exists("discountPercent"))
                .thenReturn(Mono.just(FilterResponse.of("spring-data", List.of(hit("p01")))));

        webTestClient.get().uri("/api/spring-data/products/filter/exists?field=discountPercent")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.total").isEqualTo(1);
    }

    @Test
    void emptyResultReturns200WithEmptyList() {
        when(service.term(eq("category"), eq("nonexistent")))
                .thenReturn(Mono.just(FilterResponse.of("spring-data", List.of())));

        webTestClient.get().uri("/api/spring-data/products/filter/term?field=category&value=nonexistent")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.total").isEqualTo(0)
                .jsonPath("$.hits").isArray()
                .jsonPath("$.hits.length()").isEqualTo(0);
    }

    @Test
    void composeReturns200() {
        when(service.compose(any()))
                .thenReturn(Mono.just(FilterResponse.of("spring-data", List.of(hit("p01")))));

        String body = """
                {"query":"gaming","terms":[{"field":"category","values":["laptop"]}],
                 "ranges":[{"field":"price","type":"number","lte":"2000"}]}
                """;
        webTestClient.post().uri("/api/spring-data/products/filter/compose")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.total").isEqualTo(1);
    }

    // ---- validation: HTTP 400 ----

    @Test
    void termBlankFieldReturns400AndSkipsService() {
        webTestClient.get()
                .uri(b -> b.path("/api/spring-data/products/filter/term")
                        .queryParam("field", " ").queryParam("value", "laptop").build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.title").isEqualTo("Invalid request");
        verifyNoInteractions(service);
    }

    @Test
    void termMissingValueReturns400() {
        webTestClient.get().uri("/api/spring-data/products/filter/term?field=category")
                .exchange()
                .expectStatus().isBadRequest();
        verifyNoInteractions(service);
    }

    @Test
    void existsBlankFieldReturns400() {
        webTestClient.get().uri("/api/spring-data/products/filter/exists?field=")
                .exchange()
                .expectStatus().isBadRequest();
        verifyNoInteractions(service);
    }

    @Test
    void composeEmptyTermsValuesReturns400() {
        String body = """
                {"terms":[{"field":"category","values":[]}]}
                """;
        webTestClient.post().uri("/api/spring-data/products/filter/compose")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.title").isEqualTo("Invalid request");
        verifyNoInteractions(service);
    }

    @Test
    void composeInvalidRangeTypeReturns400() {
        String body = """
                {"ranges":[{"field":"price","type":"wildcard","gte":"1"}]}
                """;
        webTestClient.post().uri("/api/spring-data/products/filter/compose")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.title").isEqualTo("Invalid request");
        verifyNoInteractions(service);
    }

    // ---- Finding #3: terms values container-element validation (compose) ----

    @Test
    void composeTermsWithNullElementReturns400() {
        String body = """
                {"terms":[{"field":"category","values":[null]}]}
                """;
        webTestClient.post().uri("/api/spring-data/products/filter/compose")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.title").isEqualTo("Invalid request");
        verifyNoInteractions(service);
    }

    @Test
    void composeTermsWithBlankElementReturns400() {
        String body = """
                {"terms":[{"field":"category","values":["   "]}]}
                """;
        webTestClient.post().uri("/api/spring-data/products/filter/compose")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.title").isEqualTo("Invalid request");
        verifyNoInteractions(service);
    }

    @Test
    void composeMustNotTermsWithBlankElementReturns400() {
        String body = """
                {"mustNotTerms":[{"field":"brand","values":[""]}]}
                """;
        webTestClient.post().uri("/api/spring-data/products/filter/compose")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.title").isEqualTo("Invalid request");
        verifyNoInteractions(service);
    }

    // ---- Finding #3: GET /terms element validation (controller-level) ----

    @Test
    void termsWithBlankElementReturns400AndSkipsService() {
        webTestClient.get()
                .uri(b -> b.path("/api/spring-data/products/filter/terms")
                        .queryParam("field", "brand")
                        .queryParam("values", "Apple").queryParam("values", "  ").build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.title").isEqualTo("Invalid request");
        verifyNoInteractions(service);
    }
}
