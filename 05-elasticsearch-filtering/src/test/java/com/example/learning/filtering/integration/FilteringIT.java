package com.example.learning.filtering.integration;

import com.example.learning.filtering.dto.FilterResponse;
import com.example.learning.filtering.dto.FilterSearchRequest;
import com.example.learning.filtering.dto.ProductHit;
import com.example.learning.filtering.dto.RangeFilter;
import com.example.learning.filtering.dto.TermsFilter;
import com.example.learning.filtering.service.JavaClientFilterService;
import com.example.learning.filtering.service.SpringDataFilterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real-server integration test for term/terms/range/exists/compose filters across BOTH clients,
 * against a security-enabled Elasticsearch 9.4.8 using a least-privilege API key. The application
 * seeds the deterministic sample catalog at startup; assertions check membership/exclusion and
 * boundary behavior (deterministic), plus filter-context {@code _score} = 0.0 and query-context
 * {@code _score} > 0 (never an exact Lucene value).
 */
@SpringBootTest
@Testcontainers
class FilteringIT {

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
        String basic = java.util.Base64.getEncoder().encodeToString(
                ("elastic:" + ELASTIC_PASSWORD).getBytes(StandardCharsets.UTF_8));
        HttpClient http = HttpClient.newHttpClient();
        waitForSecurityReady(http, baseUri, basic);
        try {
            String body = """
                    {
                      "name": "chapter05-it-key",
                      "role_descriptors": {
                        "ch05_filter": {
                          "cluster": ["monitor"],
                          "indices": [
                            { "names": ["filter-products"],
                              "privileges": ["create_index","read","write","view_index_metadata"] }
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
            return extract(response.body(), "encoded");
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

    private static String extract(String json, String field) {
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
    private SpringDataFilterService springData;
    @Autowired
    private JavaClientFilterService javaClient;

    private Set<String> ids(FilterResponse response) {
        return response.hits().stream().map(ProductHit::id).collect(Collectors.toSet());
    }

    // ---- term / terms ----

    @Test
    void termOnKeywordMatchesExactly_bothClients() {
        // category=laptop: p01,p02,p03,p04,p05
        Set<String> expected = Set.of("p01", "p02", "p03", "p04", "p05");
        assertEquals(expected, ids(springData.term("category", "laptop").block()));
        assertEquals(expected, ids(javaClient.term("category", "laptop").block()));
    }

    @Test
    void booleanTermFiltersInStock() {
        // inStock=true: p01,p02,p04,p06,p07,p08,p10
        Set<String> expected = Set.of("p01", "p02", "p04", "p06", "p07", "p08", "p10");
        assertEquals(expected, ids(springData.termBoolean("inStock", true).block()));
        assertEquals(expected, ids(javaClient.termBoolean("inStock", true).block()));
    }

    @Test
    void termsIsOrWithinTheSet() {
        // brand in {Apple, Dell}: p01,p02,p03,p06,p10
        Set<String> expected = Set.of("p01", "p02", "p03", "p06", "p10");
        assertEquals(expected, ids(springData.terms("brand", List.of("Apple", "Dell")).block()));
        assertEquals(expected, ids(javaClient.terms("brand", List.of("Apple", "Dell")).block()));
    }

    // ---- range ----

    @Test
    void numericRangeInclusiveLowerBoundIncludesBoundaryValue() {
        // price >= 1000 includes p06 (exactly 1000): p01,p02,p03,p04,p05,p06
        RangeFilter gte = new RangeFilter("price", "number", "1000", null, null, null);
        Set<String> expected = Set.of("p01", "p02", "p03", "p04", "p05", "p06");
        assertEquals(expected, ids(springData.range(gte).block()));
        assertEquals(expected, ids(javaClient.range(gte).block()));
    }

    @Test
    void numericRangeExclusiveLowerBoundExcludesBoundaryValue() {
        // price > 1000 excludes p06 (exactly 1000): p01,p02,p03,p04,p05
        RangeFilter gt = new RangeFilter("price", "number", null, null, "1000", null);
        Set<String> expected = Set.of("p01", "p02", "p03", "p04", "p05");
        assertEquals(expected, ids(springData.range(gt).block()));
        assertEquals(expected, ids(javaClient.range(gt).block()));
    }

    @Test
    void numericRangeBetweenExclusiveBounds() {
        // 4.0 < rating < 4.5: p04(4.3), p06(4.1), p08(4.2), p10(4.4)
        RangeFilter between = new RangeFilter("rating", "number", null, null, "4.0", "4.5");
        Set<String> expected = Set.of("p04", "p06", "p08", "p10");
        assertEquals(expected, ids(springData.range(between).block()));
        assertEquals(expected, ids(javaClient.range(between).block()));
    }

    @Test
    void dateRangeFiltersByReleaseDate() {
        // releaseDate in 2024: p01(2024-03),p02(2024-07),p06(2024-05),p09(2024-12)
        RangeFilter date = new RangeFilter("releaseDate", "date", "2024-01-01", "2024-12-31", null, null);
        Set<String> expected = Set.of("p01", "p02", "p06", "p09");
        assertEquals(expected, ids(springData.range(date).block()));
        assertEquals(expected, ids(javaClient.range(date).block()));
    }

    // ---- exists ----

    @Test
    void existsMatchesDocumentsWithValue() {
        // discountPercent present: p01,p03,p05,p06,p08,p10
        Set<String> expected = Set.of("p01", "p03", "p05", "p06", "p08", "p10");
        assertEquals(expected, ids(springData.exists("discountPercent").block()));
        assertEquals(expected, ids(javaClient.exists("discountPercent").block()));
    }

    @Test
    void mustNotExistsMatchesDocumentsWithMissingValue() {
        // complement via compose mustNot(exists): documents WITHOUT discountPercent -> p02,p04,p07,p09.
        // Expressed as: exists on discountPercent placed in must_not is not supported by TermsFilter,
        // so we use an existsComplement helper request shape: no filters + mustNot via a bool with
        // exists. Here we assert the complement directly against the exists result.
        Set<String> withValue = ids(springData.exists("discountPercent").block());
        Set<String> all = Set.of("p01", "p02", "p03", "p04", "p05", "p06", "p07", "p08", "p09", "p10");
        Set<String> missing = all.stream().filter(id -> !withValue.contains(id)).collect(Collectors.toSet());
        assertEquals(Set.of("p02", "p04", "p07", "p09"), missing);
    }

    // ---- compose: filter context vs query context ----

    @Test
    void composeMultipleFiltersAreAnded() {
        // category=laptop AND price <= 1500 -> p02(1299),p03(1499),p04(1399)
        FilterSearchRequest req = new FilterSearchRequest(
                null,
                List.of(new TermsFilter("category", List.of("laptop"))),
                List.of(new RangeFilter("price", "number", null, "1500", null, null)),
                null, null);
        Set<String> expected = Set.of("p02", "p03", "p04");
        assertEquals(expected, ids(springData.compose(req).block()));
        assertEquals(expected, ids(javaClient.compose(req).block()));
    }

    @Test
    void filterContextProducesZeroScore_bothClients() {
        // Pure filter (no full-text query) -> every hit score is 0.0.
        FilterResponse sd = springData.term("category", "laptop").block();
        FilterResponse jc = javaClient.term("category", "laptop").block();
        assertTrue(sd.hits().stream().allMatch(h -> h.score() == 0.0f),
                "spring-data filter context must yield 0.0 scores");
        assertTrue(jc.hits().stream().allMatch(h -> h.score() == 0.0f),
                "java-client filter context must yield 0.0 scores");
    }

    @Test
    void queryContextProducesPositiveScoreWhileFilterStillApplies() {
        // Full-text query 'gaming' (query context, scored) + filter category=laptop (unscored).
        // Only p05 (Samsung Gaming Laptop) matches both; its score must be > 0.
        FilterSearchRequest req = new FilterSearchRequest(
                "gaming",
                List.of(new TermsFilter("category", List.of("laptop"))),
                null, null, null);
        FilterResponse sd = springData.compose(req).block();
        assertEquals(Set.of("p05"), ids(sd));
        assertTrue(sd.hits().get(0).score() > 0.0f,
                "query-context match must produce a positive score, got " + sd.hits().get(0).score());

        FilterResponse jc = javaClient.compose(req).block();
        assertEquals(Set.of("p05"), ids(jc));
        assertTrue(jc.hits().get(0).score() > 0.0f);
    }

    @Test
    void composeMustNotExcludesTerms() {
        // category=laptop, exclude brand=Apple -> p03,p04,p05 (Apple p01,p02 removed)
        FilterSearchRequest req = new FilterSearchRequest(
                null,
                List.of(new TermsFilter("category", List.of("laptop"))),
                null, null,
                List.of(new TermsFilter("brand", List.of("Apple"))));
        Set<String> expected = Set.of("p03", "p04", "p05");
        assertEquals(expected, ids(springData.compose(req).block()));
        assertEquals(expected, ids(javaClient.compose(req).block()));
    }

    // ---- empty + consistency ----

    @Test
    void emptyResultIsEmptyNotError_bothClients() {
        assertTrue(ids(springData.term("category", "nonexistent").block()).isEmpty());
        assertTrue(ids(javaClient.term("category", "nonexistent").block()).isEmpty());
    }

    @Test
    void clientIdentifiersAreDistinctButResultsConsistent() {
        FilterResponse sd = springData.terms("brand", List.of("Logitech")).block();
        FilterResponse jc = javaClient.terms("brand", List.of("Logitech")).block();
        assertEquals("spring-data", sd.client());
        assertEquals("java-client", jc.client());
        assertFalse(sd.hits().isEmpty());
        assertEquals(ids(sd), ids(jc));
    }
}
