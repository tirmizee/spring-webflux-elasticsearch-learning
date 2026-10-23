package com.example.learning.apikeyauth.controller;

import com.example.learning.apikeyauth.dto.ClusterHealthResponse;
import com.example.learning.apikeyauth.dto.ClusterInfoResponse;
import com.example.learning.apikeyauth.service.SpringDataConnectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Authenticated cluster endpoints for the Spring Data Elasticsearch (reactive) approach.
 *
 * <ul>
 *     <li>{@code GET /api/spring-data/elasticsearch/health}</li>
 *     <li>{@code GET /api/spring-data/elasticsearch/cluster-info}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/spring-data/elasticsearch")
public class SpringDataConnectionController {

    private final SpringDataConnectionService service;

    public SpringDataConnectionController(SpringDataConnectionService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public Mono<ClusterHealthResponse> health() {
        return service.clusterHealth();
    }

    @GetMapping("/cluster-info")
    public Mono<ClusterInfoResponse> clusterInfo() {
        return service.clusterInfo();
    }
}
