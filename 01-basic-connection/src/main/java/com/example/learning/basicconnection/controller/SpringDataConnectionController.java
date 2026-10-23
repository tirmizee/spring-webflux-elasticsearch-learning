package com.example.learning.basicconnection.controller;

import com.example.learning.basicconnection.dto.ClusterHealthResponse;
import com.example.learning.basicconnection.dto.ClusterInfoResponse;
import com.example.learning.basicconnection.service.SpringDataConnectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST endpoints for the Spring Data Elasticsearch (reactive) approach.
 *
 * <p>The controller is intentionally thin: it only maps HTTP routes to service calls and lets
 * the reactive type flow back to WebFlux. All integration logic lives in the service.
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
