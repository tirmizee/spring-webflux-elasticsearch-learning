package com.example.learning.documentcrud.config;

import com.example.learning.documentcrud.domain.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ReactiveIndexOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Creates the {@code learning-products} index with the {@link Product} mapping at startup,
 * <strong>only if it does not already exist</strong> (idempotent). This keeps index setup explicit
 * and reproducible without index templates, aliases, or rollover (those belong to later chapters).
 *
 * <p>Runs once on startup and blocks briefly on purpose: this is a one-off bootstrap step before
 * the app serves traffic, not part of request handling, so it does not affect the reactive request
 * threads.
 */
@Component
public class IndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IndexInitializer.class);

    private final ReactiveElasticsearchOperations operations;

    public IndexInitializer(ReactiveElasticsearchOperations operations) {
        this.operations = operations;
    }

    @Override
    public void run(ApplicationArguments args) {
        ReactiveIndexOperations indexOps = operations.indexOps(Product.class);
        Boolean created = indexOps.exists()
                .flatMap(exists -> exists ? Mono.just(false) : indexOps.createWithMapping())
                .block();
        if (Boolean.TRUE.equals(created)) {
            log.info("Created index 'learning-products' with Product mapping");
        } else {
            log.info("Index 'learning-products' already exists; leaving it as is");
        }
    }
}
