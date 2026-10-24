package com.example.learning.filtering.controller;

import com.example.learning.filtering.dto.FilterResponse;
import com.example.learning.filtering.dto.ProductHit;
import com.example.learning.filtering.dto.RangeFilter;
import com.example.learning.filtering.exception.GlobalExceptionHandler;
import com.example.learning.filtering.service.JavaClientFilterService;
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

@WebFluxTest(controllers = JavaClientFilterController.class)
@Import(GlobalExceptionHandler.class)
class JavaClientFilterControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private JavaClientFilterService service;

    private static ProductHit hit(String id) {
        return new ProductHit(id, "Apple MacBook Pro", "laptop", "Apple",
                new BigDecimal("1999.00"), 4.8, LocalDate.parse("2024-03-10"), true, 10, 0.0f);
    }

    @Test
    void termReturns200WithHits() {
        when(service.term("category", "laptop"))
                .thenReturn(Mono.just(FilterResponse.of("java-client", List.of(hit("p01")))));

        webTestClient.get().uri("/api/java-client/products/filter/term?field=category&value=laptop")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.client").isEqualTo("java-client")
                .jsonPath("$.hits[0].id").isEqualTo("p01");
    }

    @Test
    void termsReturns200() {
        when(service.terms(eq("brand"), anyList()))
                .thenReturn(Mono.just(FilterResponse.of("java-client", List.of(hit("p01")))));

        webTestClient.get().uri("/api/java-client/products/filter/terms?field=brand&values=Apple,Dell")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.total").isEqualTo(1);
    }

    @Test
    void rangeReturns200() {
        when(service.range(any(RangeFilter.class)))
                .thenReturn(Mono.just(FilterResponse.of("java-client", List.of(hit("p01")))));

        webTestClient.get().uri("/api/java-client/products/filter/range?field=rating&type=number&gt=4.0&lt=4.5")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.total").isEqualTo(1);
    }

    @Test
    void existsReturns200() {
        when(service.exists("discountPercent"))
                .thenReturn(Mono.just(FilterResponse.of("java-client", List.of(hit("p01")))));

        webTestClient.get().uri("/api/java-client/products/filter/exists?field=discountPercent")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.total").isEqualTo(1);
    }

    @Test
    void emptyResultReturns200WithEmptyList() {
        when(service.term(eq("category"), eq("nonexistent")))
                .thenReturn(Mono.just(FilterResponse.of("java-client", List.of())));

        webTestClient.get().uri("/api/java-client/products/filter/term?field=category&value=nonexistent")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.total").isEqualTo(0)
                .jsonPath("$.hits.length()").isEqualTo(0);
    }

    @Test
    void composeReturns200() {
        when(service.compose(any()))
                .thenReturn(Mono.just(FilterResponse.of("java-client", List.of(hit("p01")))));

        String body = """
                {"terms":[{"field":"category","values":["laptop"]}],
                 "exists":["discountPercent"]}
                """;
        webTestClient.post().uri("/api/java-client/products/filter/compose")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.total").isEqualTo(1);
    }

    // ---- validation: HTTP 400 ----

    @Test
    void termMissingFieldReturns400() {
        webTestClient.get().uri("/api/java-client/products/filter/term?value=laptop")
                .exchange()
                .expectStatus().isBadRequest();
        verifyNoInteractions(service);
    }

    @Test
    void rangeBlankTypeReturns400() {
        webTestClient.get()
                .uri(b -> b.path("/api/java-client/products/filter/range")
                        .queryParam("field", "price").queryParam("type", " ")
                        .queryParam("gte", "1000").build())
                .exchange()
                .expectStatus().isBadRequest();
        verifyNoInteractions(service);
    }

    @Test
    void composeBlankTermsFieldReturns400() {
        String body = """
                {"terms":[{"field":"","values":["laptop"]}]}
                """;
        webTestClient.post().uri("/api/java-client/products/filter/compose")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.title").isEqualTo("Invalid request");
        verifyNoInteractions(service);
    }

    // ---- Finding #3: terms values container-element validation ----

    @Test
    void composeTermsWithBlankElementReturns400() {
        String body = """
                {"terms":[{"field":"category","values":["   "]}]}
                """;
        webTestClient.post().uri("/api/java-client/products/filter/compose")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.title").isEqualTo("Invalid request");
        verifyNoInteractions(service);
    }

    @Test
    void composeMustNotTermsWithNullElementReturns400() {
        String body = """
                {"mustNotTerms":[{"field":"brand","values":[null]}]}
                """;
        webTestClient.post().uri("/api/java-client/products/filter/compose")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.title").isEqualTo("Invalid request");
        verifyNoInteractions(service);
    }

    @Test
    void termsWithBlankElementReturns400AndSkipsService() {
        webTestClient.get()
                .uri(b -> b.path("/api/java-client/products/filter/terms")
                        .queryParam("field", "brand")
                        .queryParam("values", "Apple").queryParam("values", "  ").build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.title").isEqualTo("Invalid request");
        verifyNoInteractions(service);
    }
}
