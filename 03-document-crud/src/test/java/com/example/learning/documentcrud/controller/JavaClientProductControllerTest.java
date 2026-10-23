package com.example.learning.documentcrud.controller;

import com.example.learning.documentcrud.dto.ProductResponse;
import com.example.learning.documentcrud.exception.DuplicateProductException;
import com.example.learning.documentcrud.exception.GlobalExceptionHandler;
import com.example.learning.documentcrud.exception.ProductNotFoundException;
import com.example.learning.documentcrud.service.JavaClientProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** Mirrors the Spring Data controller test to confirm both clients expose identical behavior. */
@WebFluxTest(controllers = JavaClientProductController.class)
@Import(GlobalExceptionHandler.class)
class JavaClientProductControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private JavaClientProductService service;

    private static final String VALID_BODY = """
            {"name":"Keyboard","description":"mechanical","price":129.90,"quantity":10}
            """;

    @Test
    void createReturns201() {
        when(service.create(eq("p1"), any())).thenReturn(Mono.just(
                new ProductResponse("java-client", "p1", "Keyboard", "mechanical", new BigDecimal("129.90"), 10)));

        webTestClient.post().uri("/api/java-client/products/p1")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(VALID_BODY)
                .exchange()
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.client").isEqualTo("java-client");
    }

    @Test
    void createDuplicateReturns409() {
        when(service.create(eq("p1"), any())).thenReturn(Mono.error(new DuplicateProductException("p1")));

        webTestClient.post().uri("/api/java-client/products/p1")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(VALID_BODY)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void getMissingReturns404() {
        when(service.get("nope")).thenReturn(Mono.error(new ProductNotFoundException("nope")));

        webTestClient.get().uri("/api/java-client/products/nope")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteReturns204() {
        when(service.delete("p1")).thenReturn(Mono.empty());

        webTestClient.delete().uri("/api/java-client/products/p1")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void invalidBodyReturns400() {
        String bad = "{\"name\":\"\",\"price\":-1,\"quantity\":-5}";
        webTestClient.post().uri("/api/java-client/products/p1")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(bad)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
