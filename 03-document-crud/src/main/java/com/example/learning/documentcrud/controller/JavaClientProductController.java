package com.example.learning.documentcrud.controller;

import com.example.learning.documentcrud.dto.ProductRequest;
import com.example.learning.documentcrud.dto.ProductResponse;
import com.example.learning.documentcrud.service.JavaClientProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Product CRUD endpoints backed by the official Elasticsearch Java API Client.
 * Observable behavior mirrors the Spring Data controller exactly.
 *
 * <ul>
 *     <li>{@code POST   /api/java-client/products/{id}} → 201 Created (409 if id exists)</li>
 *     <li>{@code GET    /api/java-client/products/{id}} → 200 OK (404 if missing)</li>
 *     <li>{@code PUT    /api/java-client/products/{id}} → 200 OK (404 if missing; full replace)</li>
 *     <li>{@code DELETE /api/java-client/products/{id}} → 204 No Content (404 if missing)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/java-client/products")
public class JavaClientProductController {

    private final JavaClientProductService service;

    public JavaClientProductController(JavaClientProductService service) {
        this.service = service;
    }

    @PostMapping("/{id}")
    public Mono<ResponseEntity<ProductResponse>> create(@PathVariable String id,
                                                        @Valid @RequestBody ProductRequest request) {
        return service.create(id, request)
                .map(body -> ResponseEntity.status(HttpStatus.CREATED).body(body));
    }

    @GetMapping("/{id}")
    public Mono<ProductResponse> get(@PathVariable String id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public Mono<ProductResponse> update(@PathVariable String id,
                                        @Valid @RequestBody ProductRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return service.delete(id)
                .then(Mono.fromSupplier(() -> ResponseEntity.noContent().build()));
    }
}
