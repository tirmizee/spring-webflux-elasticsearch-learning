package com.example.learning.filtering;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for Chapter 05 - Elasticsearch Filtering.
 *
 * <p>Demonstrates {@code term}/{@code terms}, {@code range} (numeric and date), {@code exists},
 * and composed {@code bool} filters over a small product catalog ({@code filter-products} index),
 * with a focus on <strong>filter context vs query context</strong> and the effect on {@code _score}.
 * Implemented with two clients that run the same query DSL and differ only in execution:
 * <ul>
 *     <li>Spring Data Elasticsearch reactive ({@code ReactiveElasticsearchOperations.search})</li>
 *     <li>Official Elasticsearch Java API Client (synchronous, isolated on boundedElastic)</li>
 * </ul>
 */
@SpringBootApplication
public class FilteringApplication {

    public static void main(String[] args) {
        SpringApplication.run(FilteringApplication.class, args);
    }
}
