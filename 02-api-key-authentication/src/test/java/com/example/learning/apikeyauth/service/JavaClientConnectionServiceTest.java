package com.example.learning.apikeyauth.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchVersionInfo;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch.cluster.ElasticsearchClusterClient;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import com.example.learning.apikeyauth.dto.ClusterHealthResponse;
import com.example.learning.apikeyauth.dto.ClusterInfoResponse;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Java API Client service: response mapping, error propagation, and - the part
 * specific to a blocking client in WebFlux - that the blocking call is isolated onto a
 * bounded-elastic worker and is executed lazily (only on subscription).
 */
class JavaClientConnectionServiceTest {

    @Test
    void clusterHealthMapsResponse() throws IOException {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        ElasticsearchClusterClient cluster = mock(ElasticsearchClusterClient.class);
        HealthResponse health = mock(HealthResponse.class);
        when(health.clusterName()).thenReturn("secure-cluster");
        when(health.status()).thenReturn(HealthStatus.Green);
        when(health.numberOfNodes()).thenReturn(1);
        when(client.cluster()).thenReturn(cluster);
        when(cluster.health()).thenReturn(health);

        StepVerifier.create(new JavaClientConnectionService(client).clusterHealth())
                .expectNext(new ClusterHealthResponse("java-client", "secure-cluster", "green", 1, true))
                .verifyComplete();
    }

    @Test
    void clusterInfoMapsResponse() throws IOException {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        InfoResponse info = mock(InfoResponse.class);
        ElasticsearchVersionInfo version = mock(ElasticsearchVersionInfo.class);
        when(version.number()).thenReturn("9.4.8");
        when(info.clusterName()).thenReturn("secure-cluster");
        when(info.clusterUuid()).thenReturn("uuid-xyz");
        when(info.version()).thenReturn(version);
        when(info.tagline()).thenReturn("You Know, for Search");
        when(client.info()).thenReturn(info);

        StepVerifier.create(new JavaClientConnectionService(client).clusterInfo())
                .expectNext(new ClusterInfoResponse(
                        "java-client", "secure-cluster", "uuid-xyz", "9.4.8", "You Know, for Search"))
                .verifyComplete();
    }

    @Test
    void propagatesErrorFromClient() throws IOException {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        ElasticsearchClusterClient cluster = mock(ElasticsearchClusterClient.class);
        when(client.cluster()).thenReturn(cluster);
        when(cluster.health()).thenThrow(new IOException("connection refused"));

        StepVerifier.create(new JavaClientConnectionService(client).clusterHealth())
                .expectError(IOException.class)
                .verify();
    }

    @Test
    void blockingCallRunsOnBoundedElasticNotCallerThread() throws IOException {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        InfoResponse info = mock(InfoResponse.class);
        ElasticsearchVersionInfo version = mock(ElasticsearchVersionInfo.class);
        when(version.number()).thenReturn("9.4.8");
        when(info.clusterName()).thenReturn("secure-cluster");
        when(info.clusterUuid()).thenReturn("uuid-xyz");
        when(info.version()).thenReturn(version);
        when(info.tagline()).thenReturn("You Know, for Search");

        AtomicReference<String> executionThread = new AtomicReference<>();
        when(client.info()).thenAnswer(inv -> {
            executionThread.set(Thread.currentThread().getName());
            return info;
        });

        String callerThread = Thread.currentThread().getName();
        StepVerifier.create(new JavaClientConnectionService(client).clusterInfo())
                .expectNextCount(1)
                .verifyComplete();

        String actual = executionThread.get();
        assertNotNull(actual, "client.info() was never invoked");
        assertTrue(actual.startsWith("boundedElastic-"),
                "expected blocking call on a boundedElastic worker but ran on: " + actual);
        assertNotEquals(callerThread, actual, "blocking call must not run on the caller thread");
    }

    @Test
    void blockingCallIsLazyUntilSubscription() throws IOException {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        AtomicBoolean invoked = new AtomicBoolean(false);
        when(client.info()).thenAnswer(inv -> {
            invoked.set(true);
            return mock(InfoResponse.class);
        });

        // Building the Mono must not trigger the blocking call.
        new JavaClientConnectionService(client).clusterInfo();

        verify(client, never()).info();
        assertFalse(invoked.get(), "blocking call must not happen before subscription");
    }
}
