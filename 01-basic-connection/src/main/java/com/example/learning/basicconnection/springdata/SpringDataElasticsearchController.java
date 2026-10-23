package com.example.learning.basicconnection.springdata;

import com.example.learning.basicconnection.dto.ClusterHealthResponse;
import com.example.learning.basicconnection.dto.ClusterInfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST endpoints for the Spring Data Elasticsearch (reactive) approach.
 *
 * <ul>
 *     <li>{@code GET /api/spring-data/elasticsearch/health}</li>
 *     <li>{@code GET /api/spring-data/elasticsearch/cluster-info}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/spring-data/elasticsearch")
public class SpringDataElasticsearchController {

    private final SpringDataElasticsearchService service;

    public SpringDataElasticsearchController(SpringDataElasticsearchService service) {
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
