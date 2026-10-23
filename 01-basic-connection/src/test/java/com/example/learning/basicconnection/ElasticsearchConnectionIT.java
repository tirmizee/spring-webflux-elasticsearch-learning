package com.example.learning.basicconnection;

import com.example.learning.basicconnection.dto.ClusterHealthResponse;
import com.example.learning.basicconnection.dto.ClusterInfoResponse;
import com.example.learning.basicconnection.javaclient.JavaClientElasticsearchService;
import com.example.learning.basicconnection.springdata.SpringDataElasticsearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

import java.util.function.Predicate;

/**
 * Real-server integration test. Starts an actual Elasticsearch 9.4.8 node with Testcontainers
 * (security disabled to match Chapter 01's local setup) and verifies that BOTH client approaches
 * really contact the cluster and return a well-formed, mapped response.
 *
 * <p>Requires a running Docker daemon. Runs in the {@code verify} phase via the failsafe plugin
 * (class name ends with {@code IT}).
 */
@SpringBootTest
@Testcontainers
class ElasticsearchConnectionIT {

    private static final DockerImageName ES_IMAGE =
            DockerImageName.parse("elasticsearch:9.4.8")
                    .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch");

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(ES_IMAGE)
            .withEnv("xpack.security.enabled", "false")
            .withEnv("xpack.security.http.ssl.enabled", "false")
            .withEnv("discovery.type", "single-node")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    @DynamicPropertySource
    static void elasticsearchProperties(DynamicPropertyRegistry registry) {
        // getHttpHostAddress() returns host:port; security is off so we use plain http.
        registry.add("spring.elasticsearch.uris", () -> "http://" + ELASTICSEARCH.getHttpHostAddress());
    }

    @Autowired
    private SpringDataElasticsearchService springDataService;

    @Autowired
    private JavaClientElasticsearchService javaClientService;

    private static final Predicate<String> NON_BLANK = s -> s != null && !s.isBlank();

    @Test
    void springDataHealthContactsRealCluster() {
        StepVerifier.create(springDataService.clusterHealth())
                .assertNext(this::assertValidHealth)
                .verifyComplete();
    }

    @Test
    void springDataInfoReturnsRealVersion() {
        StepVerifier.create(springDataService.clusterInfo())
                .assertNext(info -> assertValidInfo(info, "spring-data"))
                .verifyComplete();
    }

    @Test
    void javaClientHealthContactsRealCluster() {
        StepVerifier.create(javaClientService.clusterHealth())
                .assertNext(this::assertValidHealth)
                .verifyComplete();
    }

    @Test
    void javaClientInfoReturnsRealVersion() {
        StepVerifier.create(javaClientService.clusterInfo())
                .assertNext(info -> assertValidInfo(info, "java-client"))
                .verifyComplete();
    }

    private void assertValidHealth(ClusterHealthResponse health) {
        if (!health.reachable()) {
            throw new AssertionError("cluster was not reachable");
        }
        if (!NON_BLANK.test(health.clusterName())) {
            throw new AssertionError("clusterName should not be blank");
        }
        // status is dynamic cluster metadata: assert it is one of the valid values, not a fixed value.
        if (!("green".equals(health.status()) || "yellow".equals(health.status()) || "red".equals(health.status()))) {
            throw new AssertionError("unexpected status: " + health.status());
        }
        if (health.numberOfNodes() < 1) {
            throw new AssertionError("expected at least 1 node");
        }
    }

    private void assertValidInfo(ClusterInfoResponse info, String expectedClient) {
        if (!expectedClient.equals(info.client())) {
            throw new AssertionError("expected client " + expectedClient + " but got " + info.client());
        }
        // The server version is deterministic for this pinned image.
        if (!"9.4.8".equals(info.versionNumber())) {
            throw new AssertionError("expected ES version 9.4.8 but got " + info.versionNumber());
        }
        if (!NON_BLANK.test(info.clusterUuid())) {
            throw new AssertionError("clusterUuid should not be blank");
        }
    }
}
