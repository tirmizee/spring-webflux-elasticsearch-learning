package com.example.learning.apikeyauth.controller;

import com.example.learning.apikeyauth.dto.ClusterHealthResponse;
import com.example.learning.apikeyauth.dto.ClusterInfoResponse;
import com.example.learning.apikeyauth.service.JavaClientConnectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Authenticated cluster endpoints for the official Elasticsearch Java API Client approach.
 *
 * <ul>
 *     <li>{@code GET /api/java-client/elasticsearch/health}</li>
 *     <li>{@code GET /api/java-client/elasticsearch/cluster-info}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/java-client/elasticsearch")
public class JavaClientConnectionController {

    private final JavaClientConnectionService service;

    public JavaClientConnectionController(JavaClientConnectionService service) {
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
