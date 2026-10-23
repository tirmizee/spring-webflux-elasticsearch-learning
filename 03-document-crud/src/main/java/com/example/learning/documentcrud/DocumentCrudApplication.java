package com.example.learning.documentcrud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for Chapter 03 - Document CRUD.
 *
 * <p>Demonstrates create / get / update / delete of {@code Product} documents in a single index
 * ({@code learning-products}) through two clients kept clearly separated:
 * <ul>
 *     <li>Spring Data Elasticsearch reactive ({@code ReactiveElasticsearchOperations})</li>
 *     <li>Official Elasticsearch Java API Client (synchronous, isolated on boundedElastic)</li>
 * </ul>
 * Both are authenticated with an API key (Chapter 02 approach) and operate on the same schema.
 */
@SpringBootApplication
public class DocumentCrudApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentCrudApplication.class, args);
    }
}
