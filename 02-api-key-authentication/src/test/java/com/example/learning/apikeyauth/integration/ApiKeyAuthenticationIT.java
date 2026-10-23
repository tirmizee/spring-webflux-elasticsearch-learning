package com.example.learning.apikeyauth.integration;

import com.example.learning.apikeyauth.dto.ClusterHealthResponse;
import com.example.learning.apikeyauth.dto.ClusterInfoResponse;
import com.example.learning.apikeyauth.service.JavaClientConnectionService;
import com.example.learning.apikeyauth.service.SpringDataConnectionService;
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Real-server integration test for API key authentication.
 *
 * <p>Starts a <strong>security-enabled</strong> Elasticsearch 9.4.8 container, creates a real API
 * key via the {@code elastic} superuser, and verifies that BOTH auto-configured clients (reactive
 * Spring Data and the Java API Client) authenticate with that key and reach the cluster.
 *
 * <p>No Elasticsearch behavior is mocked here: this proves actual authentication end to end.
 * HTTP-layer TLS is disabled to match this chapter's local docker-compose; security (and thus the
 * API key mechanism) is fully active.
 */
@SpringBootTest
@Testcontainers
class ApiKeyAuthenticationIT {

    private static final String ELASTIC_PASSWORD = "it-local-password";

    private static final DockerImageName ES_IMAGE =
            DockerImageName.parse("elasticsearch:9.4.8")
                    .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch");

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(ES_IMAGE)
            .withPassword(ELASTIC_PASSWORD)
            .withEnv("xpack.security.http.ssl.enabled", "false")
            .withEnv("discovery.type", "single-node")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    @DynamicPropertySource
    static void elasticsearchProperties(DynamicPropertyRegistry registry) {
        String uri = "http://" + ELASTICSEARCH.getHttpHostAddress();
        registry.add("spring.elasticsearch.uris", () -> uri);
        // Create a real, least-privilege API key as the elastic superuser and hand its ENCODED
        // form to the application. The app itself only ever sees the API key, never the password.
        registry.add("spring.elasticsearch.api-key", () -> createApiKey(uri));
    }

    private static String createApiKey(String baseUri) {
        String basic = Base64.getEncoder().encodeToString(
                ("elastic:" + ELASTIC_PASSWORD).getBytes(StandardCharsets.UTF_8));
        HttpClient http = HttpClient.newHttpClient();

        // The container reports "started" as soon as the HTTP port is open, but the security
        // subsystem (authenticating the elastic user) can take a few more seconds. Wait until an
        // authenticated request succeeds before creating the key.
        waitForSecurityReady(http, baseUri, basic);

        try {
            // Least-privilege: only what this chapter's endpoints need (cluster monitor for
            // health + root info). role_descriptors with cluster:["monitor"].
            String body = """
                    {
                      "name": "chapter02-it-key",
                      "role_descriptors": {
                        "ch02_reader": { "cluster": ["monitor"], "indices": [] }
                      }
                    }
                    """;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUri + "/_security/api_key"))
                    .header("Authorization", "Basic " + basic)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Failed to create API key, HTTP " + response.statusCode());
            }
            // Extract the "encoded" field (base64(id:api_key)) without a JSON dependency.
            return extractJsonField(response.body(), "encoded");
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not create API key for integration test", e);
        }
    }

    private static void waitForSecurityReady(HttpClient http, String baseUri, String basic) {
        HttpRequest health = HttpRequest.newBuilder()
                .uri(URI.create(baseUri + "/_cluster/health"))
                .header("Authorization", "Basic " + basic)
                .GET()
                .build();
        for (int attempt = 1; attempt <= 40; attempt++) {
            try {
                HttpResponse<String> r = http.send(health, HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() == 200) {
                    return;
                }
            } catch (Exception ignored) {
                // not ready yet
            }
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while waiting for ES security readiness", ie);
            }
        }
        throw new IllegalStateException("Elasticsearch security did not become ready in time");
    }

    private static String extractJsonField(String json, String field) {
        String marker = "\"" + field + "\"";
        int i = json.indexOf(marker);
        if (i < 0) {
            throw new IllegalStateException("field '" + field + "' not found in create-api-key response");
        }
        int colon = json.indexOf(':', i + marker.length());
        int firstQuote = json.indexOf('"', colon + 1);
        int secondQuote = json.indexOf('"', firstQuote + 1);
        return json.substring(firstQuote + 1, secondQuote);
    }

    @Autowired
    private SpringDataConnectionService springDataService;

    @Autowired
    private JavaClientConnectionService javaClientService;

    @Test
    void springDataAuthenticatesWithApiKey() {
        StepVerifier.create(springDataService.clusterHealth())
                .assertNext(this::assertHealthy)
                .verifyComplete();
    }

    @Test
    void springDataInfoAuthenticatesWithApiKey() {
        StepVerifier.create(springDataService.clusterInfo())
                .assertNext(info -> assertInfo(info, "spring-data"))
                .verifyComplete();
    }

    @Test
    void javaClientAuthenticatesWithApiKey() {
        StepVerifier.create(javaClientService.clusterHealth())
                .assertNext(this::assertHealthy)
                .verifyComplete();
    }

    @Test
    void javaClientInfoAuthenticatesWithApiKey() {
        StepVerifier.create(javaClientService.clusterInfo())
                .assertNext(info -> assertInfo(info, "java-client"))
                .verifyComplete();
    }

    @Test
    void invalidApiKeyIsRejectedByElasticsearchWith401() throws Exception {
        // Real negative check against the live secured cluster: a bogus API key must be rejected
        // with HTTP 401 (authentication failure), proving security is actually enforced.
        String uri = "http://" + ELASTICSEARCH.getHttpHostAddress();
        String bogusEncoded = Base64.getEncoder().encodeToString(
                "not-a-real-id:not-a-real-secret".getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri + "/_cluster/health"))
                .header("Authorization", "ApiKey " + bogusEncoded)
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 401) {
            throw new AssertionError("expected HTTP 401 for an invalid API key but got " + response.statusCode());
        }
    }

    private void assertHealthy(ClusterHealthResponse health) {
        if (!health.reachable()) {
            throw new AssertionError("cluster not reachable (authentication may have failed)");
        }
        if (health.clusterName() == null || health.clusterName().isBlank()) {
            throw new AssertionError("clusterName should not be blank");
        }
        if (!("green".equals(health.status()) || "yellow".equals(health.status()) || "red".equals(health.status()))) {
            throw new AssertionError("unexpected status: " + health.status());
        }
    }

    private void assertInfo(ClusterInfoResponse info, String expectedClient) {
        if (!expectedClient.equals(info.client())) {
            throw new AssertionError("expected client " + expectedClient + " but got " + info.client());
        }
        if (!"9.4.8".equals(info.versionNumber())) {
            throw new AssertionError("expected ES version 9.4.8 but got " + info.versionNumber());
        }
        if (info.clusterUuid() == null || info.clusterUuid().isBlank()) {
            throw new AssertionError("clusterUuid should not be blank");
        }
    }
}
