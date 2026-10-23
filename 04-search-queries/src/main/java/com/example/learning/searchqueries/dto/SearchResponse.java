package com.example.learning.searchqueries.dto;

import java.util.List;

/**
 * Stable search response. {@code client} identifies which approach served the request
 * ("spring-data" or "java-client"), {@code total} is the number of hits returned, and
 * {@code hits} is the ordered list (by descending {@code _score}). An empty search returns
 * HTTP 200 with an empty {@code hits} list — never 404.
 */
public record SearchResponse(
        String client,
        long total,
        List<ProductHit> hits
) {
    public static SearchResponse of(String client, List<ProductHit> hits) {
        return new SearchResponse(client, hits.size(), hits);
    }
}
