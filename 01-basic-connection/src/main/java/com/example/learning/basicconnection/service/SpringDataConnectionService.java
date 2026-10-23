package com.example.learning.basicconnection.service;

import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import com.example.learning.basicconnection.dto.ClusterHealthResponse;
import com.example.learning.basicconnection.dto.ClusterInfoResponse;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Approach 1: Spring Data Elasticsearch reactive client.
 *
 * <p>{@link ReactiveElasticsearchClient} is the reactive client that Spring Data Elasticsearch
 * builds on top of the official Elasticsearch Java API Client. Its methods return
 * {@link Mono}/{@link reactor.core.publisher.Flux}, so the calls are genuinely non-blocking:
 * the HTTP I/O happens on the reactor-netty event loop and we never block a request thread.
 *
 * <p>This is the key difference from the Java API Client approach
 * ({@link JavaClientConnectionService}), where the client is synchronous and we must offload the
 * blocking work to a bounded-elastic scheduler.
 */
@Service
public class SpringDataConnectionService {

    private static final String CLIENT = "spring-data";

    private final ReactiveElasticsearchClient reactiveClient;

    public SpringDataConnectionService(ReactiveElasticsearchClient reactiveClient) {
        this.reactiveClient = reactiveClient;
    }

    /**
     * Calls the Elasticsearch cluster health API reactively.
     */
    public Mono<ClusterHealthResponse> clusterHealth() {
        return reactiveClient.cluster()
                .health(h -> h)
                .map(this::toHealthResponse);
    }

    /**
     * Calls the Elasticsearch root info API reactively (equivalent to {@code GET /}).
     */
    public Mono<ClusterInfoResponse> clusterInfo() {
        return reactiveClient.info()
                .map(this::toInfoResponse);
    }

    private ClusterHealthResponse toHealthResponse(HealthResponse health) {
        return new ClusterHealthResponse(
                CLIENT,
                health.clusterName(),
                health.status().jsonValue(),
                health.numberOfNodes(),
                true
        );
    }

    private ClusterInfoResponse toInfoResponse(InfoResponse info) {
        return new ClusterInfoResponse(
                CLIENT,
                info.clusterName(),
                info.clusterUuid(),
                info.version().number(),
                info.tagline()
        );
    }
}
