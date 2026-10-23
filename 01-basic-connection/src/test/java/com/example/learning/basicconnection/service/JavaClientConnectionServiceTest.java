package com.example.learning.basicconnection.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cluster.ElasticsearchClusterClient;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import co.elastic.clients.elasticsearch._types.ElasticsearchVersionInfo;
import com.example.learning.basicconnection.dto.ClusterHealthResponse;
import com.example.learning.basicconnection.dto.ClusterInfoResponse;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for the Java API Client service.
 *
 * <p>The synchronous {@link ElasticsearchClient} is mocked, so no real Elasticsearch is needed.
 * We verify that:
 * <ul>
 *     <li>blocking responses are correctly mapped to the stable DTOs,</li>
 *     <li>the reactive wrapper emits exactly one item and completes (via {@link StepVerifier}),</li>
 *     <li>client errors are propagated as an error signal rather than swallowed.</li>
 * </ul>
 */
class JavaClientConnectionServiceTest {

    @Test
    void clusterHealthMapsResponse() throws IOException {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        ElasticsearchClusterClient clusterClient = mock(ElasticsearchClusterClient.class);
        HealthResponse health = mock(HealthResponse.class);
        when(health.clusterName()).thenReturn("unit-cluster");
        when(health.status()).thenReturn(HealthStatus.Green);
        when(health.numberOfNodes()).thenReturn(1);
        when(client.cluster()).thenReturn(clusterClient);
        when(clusterClient.health()).thenReturn(health);

        JavaClientConnectionService service = new JavaClientConnectionService(client);

        StepVerifier.create(service.clusterHealth())
                .expectNext(new ClusterHealthResponse("java-client", "unit-cluster", "green", 1, true))
                .verifyComplete();
    }

    @Test
    void clusterInfoMapsResponse() throws IOException {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        InfoResponse info = mock(InfoResponse.class);
        ElasticsearchVersionInfo version = mock(ElasticsearchVersionInfo.class);
        when(version.number()).thenReturn("9.4.8");
        when(info.clusterName()).thenReturn("unit-cluster");
        when(info.clusterUuid()).thenReturn("uuid-xyz");
        when(info.version()).thenReturn(version);
        when(info.tagline()).thenReturn("You Know, for Search");
        when(client.info()).thenReturn(info);

        JavaClientConnectionService service = new JavaClientConnectionService(client);

        StepVerifier.create(service.clusterInfo())
                .expectNext(new ClusterInfoResponse(
                        "java-client", "unit-cluster", "uuid-xyz", "9.4.8", "You Know, for Search"))
                .verifyComplete();
    }

    @Test
    void clusterHealthPropagatesError() throws IOException {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        ElasticsearchClusterClient clusterClient = mock(ElasticsearchClusterClient.class);
        when(client.cluster()).thenReturn(clusterClient);
        when(clusterClient.health()).thenThrow(new IOException("connection refused"));

        JavaClientConnectionService service = new JavaClientConnectionService(client);

        StepVerifier.create(service.clusterHealth())
                .expectError(IOException.class)
                .verify();
    }
}
