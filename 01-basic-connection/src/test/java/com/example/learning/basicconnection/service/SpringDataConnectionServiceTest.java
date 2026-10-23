package com.example.learning.basicconnection.service;

import co.elastic.clients.elasticsearch._types.ElasticsearchVersionInfo;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import com.example.learning.basicconnection.dto.ClusterHealthResponse;
import com.example.learning.basicconnection.dto.ClusterInfoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClusterClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for the Spring Data reactive service.
 *
 * <p>{@link ReactiveElasticsearchClient} is mocked and returns ready-made {@link Mono}s, so the
 * test is fully deterministic and needs no Elasticsearch. {@link StepVerifier} asserts the
 * reactive stream content (exactly one mapped DTO, then completion) and error propagation.
 */
class SpringDataConnectionServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void clusterHealthMapsResponse() {
        ReactiveElasticsearchClient client = mock(ReactiveElasticsearchClient.class);
        ReactiveElasticsearchClusterClient clusterClient = mock(ReactiveElasticsearchClusterClient.class);
        HealthResponse health = mock(HealthResponse.class);
        when(health.clusterName()).thenReturn("unit-cluster");
        when(health.status()).thenReturn(HealthStatus.Yellow);
        when(health.numberOfNodes()).thenReturn(3);
        when(client.cluster()).thenReturn(clusterClient);
        when(clusterClient.health(any(java.util.function.Function.class))).thenReturn(Mono.just(health));

        SpringDataConnectionService service = new SpringDataConnectionService(client);

        StepVerifier.create(service.clusterHealth())
                .expectNext(new ClusterHealthResponse("spring-data", "unit-cluster", "yellow", 3, true))
                .verifyComplete();
    }

    @Test
    void clusterInfoMapsResponse() {
        ReactiveElasticsearchClient client = mock(ReactiveElasticsearchClient.class);
        InfoResponse info = mock(InfoResponse.class);
        ElasticsearchVersionInfo version = mock(ElasticsearchVersionInfo.class);
        when(version.number()).thenReturn("9.4.8");
        when(info.clusterName()).thenReturn("unit-cluster");
        when(info.clusterUuid()).thenReturn("uuid-xyz");
        when(info.version()).thenReturn(version);
        when(info.tagline()).thenReturn("You Know, for Search");
        when(client.info()).thenReturn(Mono.just(info));

        SpringDataConnectionService service = new SpringDataConnectionService(client);

        StepVerifier.create(service.clusterInfo())
                .expectNext(new ClusterInfoResponse(
                        "spring-data", "unit-cluster", "uuid-xyz", "9.4.8", "You Know, for Search"))
                .verifyComplete();
    }

    @Test
    void clusterInfoPropagatesError() {
        ReactiveElasticsearchClient client = mock(ReactiveElasticsearchClient.class);
        when(client.info()).thenReturn(Mono.error(new RuntimeException("connection refused")));

        SpringDataConnectionService service = new SpringDataConnectionService(client);

        StepVerifier.create(service.clusterInfo())
                .expectErrorMatches(t -> t instanceof RuntimeException && "connection refused".equals(t.getMessage()))
                .verify();
    }
}
