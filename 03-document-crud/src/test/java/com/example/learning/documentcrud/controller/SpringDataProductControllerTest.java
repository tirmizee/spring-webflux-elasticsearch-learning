package com.example.learning.documentcrud.controller;

import com.example.learning.documentcrud.dto.ProductRequest;
import com.example.learning.documentcrud.dto.ProductResponse;
import com.example.learning.documentcrud.exception.DuplicateProductException;
import com.example.learning.documentcrud.exception.GlobalExceptionHandler;
import com.example.learning.documentcrud.exception.ProductNotFoundException;
import com.example.learning.documentcrud.service.SpringDataProductService;
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

@WebFluxTest(controllers = SpringDataProductController.class)
@Import(GlobalExceptionHandler.class)
class SpringDataProductControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private SpringDataProductService service;

    private static final String VALID_BODY = """
            {"name":"Keyboard","description":"mechanical","price":129.90,"quantity":10}
            """;

    @Test
    void createReturns201() {
        when(service.create(eq("p1"), any())).thenReturn(Mono.just(
                new ProductResponse("spring-data", "p1", "Keyboard", "mechanical", new BigDecimal("129.90"), 10)));

        webTestClient.post().uri("/api/spring-data/products/p1")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(VALID_BODY)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.client").isEqualTo("spring-data")
                .jsonPath("$.id").isEqualTo("p1")
                .jsonPath("$.name").isEqualTo("Keyboard");
    }

    @Test
    void createDuplicateReturns409() {
        when(service.create(eq("p1"), any())).thenReturn(Mono.error(new DuplicateProductException("p1")));

        webTestClient.post().uri("/api/spring-data/products/p1")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(VALID_BODY)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.title").isEqualTo("Product already exists");
    }

    @Test
    void getReturns200() {
        when(service.get("p1")).thenReturn(Mono.just(
                new ProductResponse("spring-data", "p1", "Keyboard", "mechanical", new BigDecimal("129.90"), 10)));

        webTestClient.get().uri("/api/spring-data/products/p1")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.quantity").isEqualTo(10);
    }

    @Test
    void getMissingReturns404() {
        when(service.get("nope")).thenReturn(Mono.error(new ProductNotFoundException("nope")));

        webTestClient.get().uri("/api/spring-data/products/nope")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.title").isEqualTo("Product not found");
    }

    @Test
    void updateMissingReturns404() {
        when(service.update(eq("nope"), any())).thenReturn(Mono.error(new ProductNotFoundException("nope")));

        webTestClient.put().uri("/api/spring-data/products/nope")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(VALID_BODY)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteReturns204() {
        when(service.delete("p1")).thenReturn(Mono.empty());

        webTestClient.delete().uri("/api/spring-data/products/p1")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void deleteMissingReturns404() {
        when(service.delete("nope")).thenReturn(Mono.error(new ProductNotFoundException("nope")));

        webTestClient.delete().uri("/api/spring-data/products/nope")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void invalidBodyReturns400() {
        // blank name, negative price/quantity
        String bad = "{\"name\":\"\",\"price\":-1,\"quantity\":-5}";
        webTestClient.post().uri("/api/spring-data/products/p1")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(bad)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.title").isEqualTo("Invalid request");
    }
}
