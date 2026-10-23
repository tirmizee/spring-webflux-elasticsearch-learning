package com.example.learning.apikeyauth.service;

import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import com.example.learning.apikeyauth.dto.ClusterHealthResponse;
import com.example.learning.apikeyauth.dto.ClusterInfoResponse;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Approach 1: Spring Data Elasticsearch reactive client.
 *
 * <p>Identical in shape to Chapter 01, but here the {@link ReactiveElasticsearchClient} is
 * auto-configured with an API key (via {@code spring.elasticsearch.api-key}). Every request
 * therefore carries an {@code Authorization: ApiKey <encoded>} header. The calls are genuinely
 * non-blocking (reactor-netty event loop); we never block a request thread.
 */
@Service
public class SpringDataConnectionService {

    private static final String CLIENT = "spring-data";

    private final ReactiveElasticsearchClient reactiveClient;

    public SpringDataConnectionService(ReactiveElasticsearchClient reactiveClient) {
        this.reactiveClient = reactiveClient;
    }

    public Mono<ClusterHealthResponse> clusterHealth() {
        return reactiveClient.cluster()
                .health(h -> h)
                .map(this::toHealthResponse);
    }

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
