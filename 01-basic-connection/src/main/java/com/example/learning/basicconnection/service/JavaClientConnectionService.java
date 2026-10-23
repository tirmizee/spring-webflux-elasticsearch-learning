package com.example.learning.basicconnection.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import com.example.learning.basicconnection.dto.ClusterHealthResponse;
import com.example.learning.basicconnection.dto.ClusterInfoResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;

/**
 * Approach 2: official Elasticsearch Java API Client ({@link ElasticsearchClient}).
 *
 * <p><strong>Important blocking vs reactive note:</strong> {@link ElasticsearchClient} is a
 * <em>synchronous</em> client. Its methods block the calling thread while the HTTP request is
 * in flight. In a WebFlux application we must never block the reactor-netty event-loop thread,
 * otherwise we starve the small, fixed-size event-loop pool and degrade the whole server.
 *
 * <p>Therefore we wrap each blocking call in {@link Mono#fromCallable(java.util.concurrent.Callable)}
 * and move it onto {@link Schedulers#boundedElastic()} with {@code subscribeOn}. The bounded-elastic
 * scheduler is designed exactly for this: wrapping blocking/legacy APIs on a separate, bounded
 * thread pool so the event loop stays free.
 *
 * <p>Note that wrapping the <em>result</em> in {@code Mono.just(client.call())} would NOT help —
 * that still executes the blocking call on the subscribing thread before the Mono is created.
 * The blocking work must live inside the {@code fromCallable} lambda so the scheduler can run it.
 * The returned {@code Mono} is therefore non-blocking <em>to its subscriber</em>, but the underlying
 * client call itself is still blocking work that we have deliberately isolated.
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
