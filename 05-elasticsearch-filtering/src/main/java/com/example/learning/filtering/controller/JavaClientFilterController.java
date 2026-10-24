package com.example.learning.filtering.controller;

import com.example.learning.filtering.dto.FilterResponse;
import com.example.learning.filtering.dto.FilterSearchRequest;
import com.example.learning.filtering.dto.RangeFilter;
import com.example.learning.filtering.service.JavaClientFilterService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Filter endpoints backed by the official Elasticsearch Java API Client. Observable behavior
 * mirrors the Spring Data controller exactly (same queries, same response shape, same validation).
 *
 * <p>Required parameters must be present and non-blank; a missing or blank parameter yields HTTP
 * 400 (via {@link ServerWebInputException}, handled centrally) before any Elasticsearch call.
 *
 * <ul>
 *     <li>{@code GET  /api/java-client/products/filter/term?field=&value=}</li>
 *     <li>{@code GET  /api/java-client/products/filter/term/boolean?field=&value=}</li>
 *     <li>{@code GET  /api/java-client/products/filter/terms?field=&values=a,b,c}</li>
 *     <li>{@code GET  /api/java-client/products/filter/range?field=&type=&gte=&lte=&gt=&lt=}</li>
 *     <li>{@code GET  /api/java-client/products/filter/exists?field=}</li>
 *     <li>{@code POST /api/java-client/products/filter/compose}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/java-client/products/filter")
public class JavaClientFilterController {

    private final JavaClientFilterService service;

    public JavaClientFilterController(JavaClientFilterService service) {
        this.service = service;
    }

    @GetMapping("/term")
    public Mono<FilterResponse> term(@RequestParam String field, @RequestParam String value) {
        requireNonBlank("field", field);
        requireNonBlank("value", value);
        return service.term(field, value);
    }

    @GetMapping("/term/boolean")
    public Mono<FilterResponse> termBoolean(@RequestParam String field, @RequestParam boolean value) {
        requireNonBlank("field", field);
        return service.termBoolean(field, value);
    }

    @GetMapping("/terms")
    public Mono<FilterResponse> terms(@RequestParam String field, @RequestParam List<String> values) {
        requireNonBlank("field", field);
        requireNonEmpty("values", values);
        return service.terms(field, values);
    }

    @GetMapping("/range")
    public Mono<FilterResponse> range(
            @RequestParam String field,
            @RequestParam String type,
            @RequestParam(required = false) String gte,
            @RequestParam(required = false) String lte,
            @RequestParam(required = false) String gt,
            @RequestParam(required = false) String lt) {
        requireNonBlank("field", field);
        requireNonBlank("type", type);
        return service.range(new RangeFilter(field, type, gte, lte, gt, lt));
    }

    @GetMapping("/exists")
    public Mono<FilterResponse> exists(@RequestParam String field) {
        requireNonBlank("field", field);
        return service.exists(field);
    }

    @PostMapping("/compose")
    public Mono<FilterResponse> compose(@Valid @RequestBody FilterSearchRequest request) {
        return service.compose(request);
    }

    private static void requireNonBlank(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new ServerWebInputException(name + " must not be blank");
        }
    }

    private static void requireNonEmpty(String name, List<String> values) {
        if (values == null || values.isEmpty() || values.stream().allMatch(v -> v == null || v.isBlank())) {
            throw new ServerWebInputException(name + " must not be empty");
        }
    }
}
