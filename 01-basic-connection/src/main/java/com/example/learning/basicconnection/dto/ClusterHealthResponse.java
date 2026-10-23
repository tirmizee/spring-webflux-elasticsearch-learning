package com.example.learning.basicconnection.dto;

/**
 * Stable DTO for a cluster health response.
 *
 * <p>We expose this stable shape rather than the raw vendor payload so the API contract
 * does not change when Elasticsearch internals change.
 *
 * @param client      which client produced this response ("spring-data" or "java-client")
 * @param clusterName the actual cluster name reported by Elasticsearch
 * @param status      cluster health status: green / yellow / red
 * @param numberOfNodes number of nodes in the cluster
 * @param reachable   true if Elasticsearch was actually contacted successfully
 */
public record ClusterHealthResponse(
        String client,
        String clusterName,
        String status,
        int numberOfNodes,
        boolean reachable
) {
}
