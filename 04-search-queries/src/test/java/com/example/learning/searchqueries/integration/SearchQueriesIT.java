package com.example.learning.searchqueries.integration;

import com.example.learning.searchqueries.dto.BoolSearchRequest;
import com.example.learning.searchqueries.dto.ProductHit;
import com.example.learning.searchqueries.dto.QueryClause;
import com.example.learning.searchqueries.dto.SearchResponse;
import com.example.learning.searchqueries.service.JavaClientSearchService;
import com.example.learning.searchqueries.service.SpringDataSearchService;
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
 * Real-server integration test for term/match/bool queries across BOTH clients, against a
 * security-enabled Elasticsearch 9.4.8 using a least-privilege API key. The application seeds the
 * deterministic sample catalog at startup; assertions check membership/exclusion (not exact
 * {@code _score}) so they are robust against relevance-ranking details.
 */
@SpringBootTest
@Testcontainers
class SearchQueriesIT {

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
                      "name": "chapter04-it-key",
                      "role_descriptors": {
                        "ch04_search": {
                          "cluster": ["monitor"],
                          "indices": [
                            { "names": ["search-products"],
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
    private SpringDataSearchService springData;
    @Autowired
    private JavaClientSearchService javaClient;

    private Set<String> ids(SearchResponse response) {
        return response.hits().stream().map(ProductHit::id).collect(Collectors.toSet());
    }

    // ---- term ----

    @Test
    void termOnKeywordMatchesExactly_bothClients() {
        // category=laptop: p01,p02,p03,p04,p10 (all keyword 'laptop'); monitors/accessories excluded.
        Set<String> expected = Set.of("p01", "p02", "p03", "p04", "p10");
        assertEquals(expected, ids(springData.term("category", "laptop").block()));
        assertEquals(expected, ids(javaClient.term("category", "laptop").block()));
    }

    @Test
    void termExactMatchFailsOnDifferentValueOrCase() {
        // keyword term is case/exact sensitive: 'Laptop' and 'Apple ' do not match stored values.
        assertTrue(ids(springData.term("category", "Laptop").block()).isEmpty());
        assertTrue(ids(javaClient.term("brand", "apple").block()).isEmpty());
    }

    // ---- match ----

    @Test
    void matchFullTextFindsAnalyzedTokens_bothClients() {
        // match on name 'macbook' finds the two Apple MacBooks (analyzed text), both clients agree.
        Set<String> sd = ids(springData.match("name", "macbook").block());
        Set<String> jc = ids(javaClient.match("name", "macbook").block());
        assertEquals(Set.of("p01", "p02"), sd);
        assertEquals(sd, jc);
    }

    @Test
    void matchFindsByDescriptionToken() {
        // 'wireless' appears in descriptions of p05, p06, p09.
        assertEquals(Set.of("p05", "p06", "p09"), ids(springData.match("description", "wireless").block()));
    }

    // ---- bool ----

    @Test
    void boolMustNarrowsResults() {
        // must: category=laptop AND match name 'gaming' -> only p10 (Samsung Gaming Laptop).
        BoolSearchRequest req = new BoolSearchRequest(
                List.of(new QueryClause("term", "category", "laptop"),
                        new QueryClause("match", "name", "gaming")),
                null, null, null);
        assertEquals(Set.of("p10"), ids(springData.bool(req).block()));
        assertEquals(Set.of("p10"), ids(javaClient.bool(req).block()));
    }

    @Test
    void boolMustNotExcludes() {
        // category=laptop, exclude brand=Apple -> p03,p04,p10 (Apple p01,p02 removed).
        BoolSearchRequest req = new BoolSearchRequest(
                List.of(new QueryClause("term", "category", "laptop")),
                null,
                List.of(new QueryClause("term", "brand", "Apple")),
                null);
        Set<String> expected = Set.of("p03", "p04", "p10");
        assertEquals(expected, ids(springData.bool(req).block()));
        assertEquals(expected, ids(javaClient.bool(req).block()));
    }

    @Test
    void boolShouldIsOptionalWhenMustPresent() {
        // must category=laptop; should match name 'pro'. With a must clause and no
        // minimum_should_match, 'should' only boosts score -> full laptop set still returned.
        BoolSearchRequest req = new BoolSearchRequest(
                List.of(new QueryClause("term", "category", "laptop")),
                List.of(new QueryClause("match", "name", "pro")),
                null, null);
        assertEquals(Set.of("p01", "p02", "p03", "p04", "p10"), ids(springData.bool(req).block()));
    }

    @Test
    void minimumShouldMatchFiltersResultSet() {
        // must category=laptop; should name 'pro' OR name 'air'; minimumShouldMatch=1
        // -> only laptops whose name matches 'pro' or 'air': p01 (Pro), p02 (Air).
        BoolSearchRequest req = new BoolSearchRequest(
                List.of(new QueryClause("term", "category", "laptop")),
                List.of(new QueryClause("match", "name", "pro"),
                        new QueryClause("match", "name", "air")),
                null, 1);
        Set<String> expected = Set.of("p01", "p02");
        assertEquals(expected, ids(springData.bool(req).block()));
        assertEquals(expected, ids(javaClient.bool(req).block()));
    }

    @Test
    void shouldOnlyActsLikeOr() {
        // No must; should brand=Apple OR brand=Dell (term). Default minimum_should_match for a
        // pure should query is 1 -> Apple + Dell docs: p01,p02,p03,p08,p09.
        BoolSearchRequest req = new BoolSearchRequest(
                null,
                List.of(new QueryClause("term", "brand", "Apple"),
                        new QueryClause("term", "brand", "Dell")),
                null, null);
        assertEquals(Set.of("p01", "p02", "p03", "p08", "p09"), ids(springData.bool(req).block()));
    }

    // ---- empty + consistency ----

    @Test
    void emptyResultIsEmptyNotError_bothClients() {
        assertTrue(ids(springData.match("description", "zzznonexistent").block()).isEmpty());
        assertTrue(ids(javaClient.match("description", "zzznonexistent").block()).isEmpty());
    }

    @Test
    void clientIdentifiersAreDistinctButResultsConsistent() {
        SearchResponse sd = springData.term("brand", "Logitech").block();
        SearchResponse jc = javaClient.term("brand", "Logitech").block();
        assertEquals("spring-data", sd.client());
        assertEquals("java-client", jc.client());
        assertFalse(sd.hits().isEmpty());
        assertEquals(ids(sd), ids(jc));
    }
}
