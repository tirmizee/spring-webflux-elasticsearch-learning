package com.example.learning.apikeyauth.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import com.example.learning.apikeyauth.dto.ClusterHealthResponse;
import com.example.learning.apikeyauth.dto.ClusterInfoResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;

/**
 * Approach 2: official Elasticsearch Java API Client ({@link ElasticsearchClient}).
 *
 * <p>Auto-configured with the same API key as the reactive client. {@link ElasticsearchClient}
 * is <strong>synchronous</strong>: its methods block the calling thread while the HTTP request is
 * in flight. In WebFlux we must never block the reactor-netty event loop, so each call is wrapped
 * in {@link Mono#fromCallable(java.util.concurrent.Callable)} and moved onto
 * {@link Schedulers#boundedElastic()} with {@code subscribeOn}.
 *
 * <p>Note: wrapping the <em>result</em> in {@code Mono.just(client.call())} would NOT help — the
 * blocking work must live inside the {@code fromCallable} lambda so the scheduler can run it off
 * the event loop. The returned {@code Mono} is non-blocking to its subscriber; the underlying call
 * is still blocking work we have deliberately isolated.
 */
@Service
public class JavaClientConnectionService {

    private static final String CLIENT = "java-client";

    private final ElasticsearchClient elasticsearchClient;

    public JavaClientConnectionService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    public Mono<ClusterHealthResponse> clusterHealth() {
        return Mono.fromCallable(this::blockingHealth)
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<ClusterInfoResponse> clusterInfo() {
        return Mono.fromCallable(this::blockingInfo)
                .subscribeOn(Schedulers.boundedElastic());
    }

    private ClusterHealthResponse blockingHealth() throws IOException {
        HealthResponse health = elasticsearchClient.cluster().health();
        return new ClusterHealthResponse(
                CLIENT,
                health.clusterName(),
                health.status().jsonValue(),
                health.numberOfNodes(),
                true
        );
    }

    private ClusterInfoResponse blockingInfo() throws IOException {
        InfoResponse info = elasticsearchClient.info();
        return new ClusterInfoResponse(
                CLIENT,
                info.clusterName(),
                info.clusterUuid(),
                info.version().number(),
                info.tagline()
        );
    }
}
