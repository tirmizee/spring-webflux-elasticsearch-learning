package com.example.learning.basicconnection.javaclient;

import com.example.learning.basicconnection.dto.ClusterHealthResponse;
import com.example.learning.basicconnection.dto.ClusterInfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST endpoints for the official Elasticsearch Java API Client approach.
 *
 * <ul>
 *     <li>{@code GET /api/java-client/elasticsearch/health}</li>
 *     <li>{@code GET /api/java-client/elasticsearch/cluster-info}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/java-client/elasticsearch")
public class JavaClientElasticsearchController {

    private final JavaClientElasticsearchService service;

    public JavaClientElasticsearchController(JavaClientElasticsearchService service) {
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
