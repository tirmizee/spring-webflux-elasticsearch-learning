package com.example.learning.documentcrud.integration;

import com.example.learning.documentcrud.dto.ProductRequest;
import com.example.learning.documentcrud.dto.ProductResponse;
import com.example.learning.documentcrud.exception.DuplicateProductException;
import com.example.learning.documentcrud.exception.ProductNotFoundException;
import com.example.learning.documentcrud.service.JavaClientProductService;
import com.example.learning.documentcrud.service.SpringDataProductService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Real-server integration test for document CRUD across BOTH clients, against a security-enabled
 * Elasticsearch 9.4.8 using a least-privilege API key (cluster monitor + full CRUD on
 * {@code learning-products}).
 *
 * <p>The flow deliberately crosses clients: create via Spring Data, read via the Java API Client,
 * update, re-read, delete, verify gone — proving both operate on the same index/schema. Also
 * verifies duplicate-id (409 path) and missing-document (404 path) behavior. Nothing is mocked.
 */
@SpringBootTest
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductCrudIT {

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
    static void properties(DynamicPropertyRegistry registry) {
        String uri = "http://" + ELASTICSEARCH.getHttpHostAddress();
        registry.add("spring.elasticsearch.uris", () -> uri);
        registry.add("spring.elasticsearch.api-key", () -> createApiKey(uri));
    }

    private static String createApiKey(String baseUri) {
        String basic = Base64.getEncoder().encodeToString(
                ("elastic:" + ELASTIC_PASSWORD).getBytes(StandardCharsets.UTF_8));
        HttpClient http = HttpClient.newHttpClient();
        waitForSecurityReady(http, baseUri, basic);
        try {
            // Least-privilege for this chapter: monitor the cluster + full CRUD on the one index.
            String body = """
                    {
                      "name": "chapter03-it-key",
                      "role_descriptors": {
                        "ch03_crud": {
                          "cluster": ["monitor"],
                          "indices": [
                            { "names": ["learning-products"],
                              "privileges": ["create_index","read","write","delete","view_index_metadata"] }
                          ]
                        }
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
                .header("Authorization", "Basic " + basic).GET().build();
        for (int attempt = 1; attempt <= 40; attempt++) {
            try {
                if (http.send(health, HttpResponse.BodyHandlers.ofString()).statusCode() == 200) {
                    return;
                }
            } catch (Exception ignored) {
                // not ready yet
            }
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted waiting for ES", ie);
            }
        }
        throw new IllegalStateException("Elasticsearch security did not become ready in time");
    }

    private static String extractJsonField(String json, String field) {
        String marker = "\"" + field + "\"";
        int i = json.indexOf(marker);
        if (i < 0) {
            throw new IllegalStateException("field '" + field + "' not found");
        }
        int colon = json.indexOf(':', i + marker.length());
        int q1 = json.indexOf('"', colon + 1);
        int q2 = json.indexOf('"', q1 + 1);
        return json.substring(q1 + 1, q2);
    }

    @Autowired
    private SpringDataProductService springData;
    @Autowired
    private JavaClientProductService javaClient;

    private static final String ID = "it-product-1";
    private final ProductRequest create =
            new ProductRequest("Keyboard", "mechanical", new BigDecimal("129.90"), 10);
    private final ProductRequest updated =
            new ProductRequest("Keyboard Pro", "mechanical, hot-swappable", new BigDecimal("159.00"), 7);

    @Test
    @Order(1)
    void createViaSpringData() {
        StepVerifier.create(springData.create(ID, create))
                .assertNext(r -> {
                    if (!ID.equals(r.id()) || !"Keyboard".equals(r.name())) {
                        throw new AssertionError("unexpected create response: " + r);
                    }
                })
                .verifyComplete();
    }

    @Test
    @Order(2)
    void readViaJavaClientSeesSpringDataWrite() {
        StepVerifier.create(javaClient.get(ID))
                .assertNext(r -> {
                    if (!"Keyboard".equals(r.name()) || r.quantity() != 10) {
                        throw new AssertionError("java-client did not see spring-data write: " + r);
                    }
                })
                .verifyComplete();
    }

    @Test
    @Order(3)
    void duplicateCreateIsRejected() {
        StepVerifier.create(javaClient.create(ID, create))
                .expectError(DuplicateProductException.class)
                .verify();
    }

    @Test
    @Order(4)
    void updateViaJavaClient() {
        StepVerifier.create(javaClient.update(ID, updated))
                .assertNext(r -> {
                    if (!"Keyboard Pro".equals(r.name()) || r.quantity() != 7) {
                        throw new AssertionError("unexpected update response: " + r);
                    }
                })
                .verifyComplete();
    }

    @Test
    @Order(5)
    void readUpdatedViaSpringData() {
        StepVerifier.create(springData.get(ID))
                .assertNext(r -> {
                    if (!"Keyboard Pro".equals(r.name())
                            || r.price().compareTo(new BigDecimal("159.00")) != 0
                            || r.quantity() != 7) {
                        throw new AssertionError("spring-data did not see java-client update: " + r);
                    }
                })
                .verifyComplete();
    }

    @Test
    @Order(6)
    void deleteViaSpringDataThenGone() {
        StepVerifier.create(springData.delete(ID)).verifyComplete();
        StepVerifier.create(javaClient.get(ID))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    @Order(7)
    void updateMissingIsNotFound() {
        StepVerifier.create(springData.update("does-not-exist", updated))
                .expectError(ProductNotFoundException.class)
                .verify();
    }
}
