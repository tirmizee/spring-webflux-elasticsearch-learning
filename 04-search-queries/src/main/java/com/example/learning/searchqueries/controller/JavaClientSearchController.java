package com.example.learning.searchqueries.controller;

import com.example.learning.searchqueries.dto.BoolSearchRequest;
import com.example.learning.searchqueries.dto.SearchResponse;
import com.example.learning.searchqueries.service.JavaClientSearchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Search endpoints backed by the official Elasticsearch Java API Client. Observable behavior
 * mirrors the Spring Data controller exactly (same queries, same response shape).
 *
 * <ul>
 *     <li>{@code GET  /api/java-client/products/search/term?field=&value=}</li>
 *     <li>{@code GET  /api/java-client/products/search/match?field=&text=}</li>
 *     <li>{@code POST /api/java-client/products/search/bool}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/java-client/products/search")
public class JavaClientSearchController {

    private final JavaClientSearchService service;

    public JavaClientSearchController(JavaClientSearchService service) {
        this.service = service;
    }

    @GetMapping("/term")
    public Mono<SearchResponse> term(@RequestParam String field, @RequestParam String value) {
        return service.term(field, value);
    }

    @GetMapping("/match")
    public Mono<SearchResponse> match(@RequestParam String field, @RequestParam String text) {
        return service.match(field, text);
    }

    @PostMapping("/bool")
    public Mono<SearchResponse> bool(@Valid @RequestBody BoolSearchRequest request) {
        return service.bool(request);
    }
}
