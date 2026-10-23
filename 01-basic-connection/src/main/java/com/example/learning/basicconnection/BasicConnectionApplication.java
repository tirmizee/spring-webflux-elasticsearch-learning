package com.example.learning.basicconnection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for Chapter 01 - Basic Connection.
 *
 * <p>Demonstrates connecting to a local, unauthenticated Elasticsearch node using two
 * distinct approaches that are kept clearly separated:
 * <ul>
 *     <li>Spring Data Elasticsearch (reactive) - see {@code springdata} package</li>
 *     <li>Official Elasticsearch Java API Client (synchronous/blocking) - see {@code javaclient} package</li>
 * </ul>
 */
@SpringBootApplication
public class BasicConnectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(BasicConnectionApplication.class, args);
    }
}
