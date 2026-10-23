package com.example.learning.searchqueries;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for Chapter 04 - Search Queries.
 *
 * <p>Demonstrates Elasticsearch {@code term}, {@code match}, and {@code bool} queries over a small
 * product catalog ({@code search-products} index), implemented with two clients that run the same
 * query DSL and differ only in execution:
 * <ul>
 *     <li>Spring Data Elasticsearch reactive ({@code ReactiveElasticsearchOperations.search})</li>
 *     <li>Official Elasticsearch Java API Client (synchronous, isolated on boundedElastic)</li>
 * </ul>
 */
@SpringBootApplication
public class SearchQueriesApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchQueriesApplication.class, args);
    }
}
