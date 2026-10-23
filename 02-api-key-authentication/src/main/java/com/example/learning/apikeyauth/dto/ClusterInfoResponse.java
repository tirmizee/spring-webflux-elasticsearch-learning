package com.example.learning.apikeyauth.dto;

/**
 * Stable DTO for basic cluster information (the response of the ES root endpoint "/").
 *
 * @param client        which client produced this response ("spring-data" or "java-client")
 * @param clusterName   the actual cluster name reported by Elasticsearch
 * @param clusterUuid   the cluster UUID
 * @param versionNumber the Elasticsearch version number, e.g. "9.4.8"
 * @param tagline       the Elasticsearch tagline
 */
public record ClusterInfoResponse(
        String client,
        String clusterName,
        String clusterUuid,
        String versionNumber,
        String tagline
) {
}
