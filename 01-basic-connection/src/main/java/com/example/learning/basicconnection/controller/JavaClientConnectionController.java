package com.example.learning.basicconnection.controller;

import com.example.learning.basicconnection.dto.ClusterHealthResponse;
import com.example.learning.basicconnection.dto.ClusterInfoResponse;
import com.example.learning.basicconnection.service.JavaClientConnectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST endpoints for the official Elasticsearch Java API Client approach.
 *
 * <p>The controller is intentionally thin: it only maps HTTP routes to service calls. The
 * service is responsible for offloading the synchronous client's blocking work to a
 * bounded-elastic scheduler.
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
