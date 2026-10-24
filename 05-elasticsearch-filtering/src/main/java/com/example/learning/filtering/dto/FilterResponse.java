package com.example.learning.filtering.dto;

import java.util.List;

/**
 * Stable filter response. {@code client} identifies which approach served the request
 * ("spring-data" or "java-client"), {@code total} is the number of hits returned, and
 * {@code hits} is the list of matching products. An empty result returns HTTP 200 with an empty
 * {@code hits} list — never 404.
 *
 * <p>In pure filter context every hit's {@code score} is {@code 0.0}; when a full-text query is
 * combined with filters, matching documents carry a positive {@code score}.
 */
public record FilterResponse(
        String client,
        long total,
        List<ProductHit> hits
) {
    public static FilterResponse of(String client, List<ProductHit> hits) {
        return new FilterResponse(client, hits.size(), hits);
    }
}
