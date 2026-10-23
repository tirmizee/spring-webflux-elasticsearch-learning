package com.example.learning.apikeyauth.service;

import co.elastic.clients.elasticsearch._types.ElasticsearchVersionInfo;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import com.example.learning.apikeyauth.dto.ClusterHealthResponse;
import com.example.learning.apikeyauth.dto.ClusterInfoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClusterClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Spring Data reactive service: response mapping, reactive completion, and
 * error propagation. The {@link ReactiveElasticsearchClient} is mocked so the test is
 * deterministic and needs no Elasticsearch.
 */
class SpringDataConnectionServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void clusterHealthMapsResponse() {
        ReactiveElasticsearchClient client = mock(ReactiveElasticsearchClient.class);
        ReactiveElasticsearchClusterClient cluster = mock(ReactiveElasticsearchClusterClient.class);
        HealthResponse health = mock(HealthResponse.class);
        when(health.clusterName()).thenReturn("secure-cluster");
        when(health.status()).thenReturn(HealthStatus.Yellow);
        when(health.numberOfNodes()).thenReturn(2);
        when(client.cluster()).thenReturn(cluster);
        when(cluster.health(any(java.util.function.Function.class))).thenReturn(Mono.just(health));

        StepVerifier.create(new SpringDataConnectionService(client).clusterHealth())
                .expectNext(new ClusterHealthResponse("spring-data", "secure-cluster", "yellow", 2, true))
                .verifyComplete();
    }

    @Test
    void clusterInfoMapsResponse() {
        ReactiveElasticsearchClient client = mock(ReactiveElasticsearchClient.class);
        InfoResponse info = mock(InfoResponse.class);
        ElasticsearchVersionInfo version = mock(ElasticsearchVersionInfo.class);
        when(version.number()).thenReturn("9.4.8");
        when(info.clusterName()).thenReturn("secure-cluster");
        when(info.clusterUuid()).thenReturn("uuid-xyz");
        when(info.version()).thenReturn(version);
        when(info.tagline()).thenReturn("You Know, for Search");
        when(client.info()).thenReturn(Mono.just(info));

        StepVerifier.create(new SpringDataConnectionService(client).clusterInfo())
                .expectNext(new ClusterInfoResponse(
                        "spring-data", "secure-cluster", "uuid-xyz", "9.4.8", "You Know, for Search"))
                .verifyComplete();
    }

    @Test
    void propagatesError() {
        ReactiveElasticsearchClient client = mock(ReactiveElasticsearchClient.class);
        when(client.info()).thenReturn(Mono.error(new RuntimeException("boom")));

        StepVerifier.create(new SpringDataConnectionService(client).clusterInfo())
                .expectErrorMatches(t -> t instanceof RuntimeException && "boom".equals(t.getMessage()))
                .verify();
    }
}
