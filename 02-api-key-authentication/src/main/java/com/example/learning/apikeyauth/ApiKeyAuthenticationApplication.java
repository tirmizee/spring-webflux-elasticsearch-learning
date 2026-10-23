package com.example.learning.apikeyauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for Chapter 02 - API Key Authentication.
 *
 * <p>Demonstrates connecting to a <strong>security-enabled</strong> Elasticsearch node using an
 * API key, with two distinct clients kept clearly separated:
 * <ul>
 *     <li>Spring Data Elasticsearch (reactive) - see {@code controller}/{@code service}</li>
 *     <li>Official Elasticsearch Java API Client (synchronous/blocking)</li>
 * </ul>
 *
 * <p>Both clients are authenticated purely through Spring Boot auto-configuration using the
 * {@code spring.elasticsearch.api-key} property (supplied via the {@code ELASTICSEARCH_API_KEY}
 * environment variable). No custom client beans are required.
 */
@SpringBootApplication
public class ApiKeyAuthenticationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiKeyAuthenticationApplication.class, args);
    }
}
