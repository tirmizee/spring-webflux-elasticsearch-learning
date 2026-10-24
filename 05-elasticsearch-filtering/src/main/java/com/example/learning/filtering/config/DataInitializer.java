package com.example.learning.filtering.config;

import com.example.learning.filtering.data.SampleProducts;
import com.example.learning.filtering.domain.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ReactiveIndexOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Prepares the {@code filter-products} index at startup, <strong>non-destructively</strong>:
 * <ul>
 *     <li>If the index does not exist, create it with the {@link Product} mapping and seed the
 *         deterministic sample catalog ({@link SampleProducts}).</li>
 *     <li>If the index already exists, leave it and its documents untouched (no delete, no
 *         re-seed, no overwrite).</li>
 * </ul>
 *
 * <p>This runs once on startup and blocks briefly on purpose — it is a one-off bootstrap step, not
 * part of request handling, so it does not block reactor-netty request threads. Seeding uses
 * {@link RefreshPolicy#IMMEDIATE} so the sample data is searchable as soon as the app is up (useful
 * for a learning/demo context). To reset the dataset, delete the index (see the README) and restart.
 */
@Slf4j
@Component
public class DataInitializer implements ApplicationRunner {

    private final ReactiveElasticsearchOperations operations;

    public DataInitializer(ReactiveElasticsearchOperations operations) {
        this.operations = operations;
    }

    @Override
    public void run(ApplicationArguments args) {
        ReactiveIndexOperations indexOps = operations.indexOps(Product.class);
        Boolean seeded = indexOps.exists()
                .flatMap(exists -> exists ? Mono.just(false) : createAndSeed(indexOps))
                .block();
        if (Boolean.TRUE.equals(seeded)) {
            log.info("Created index 'filter-products' and seeded {} sample products",
                    SampleProducts.all().size());
        } else {
            log.info("Index 'filter-products' already exists; left unchanged (no re-seed)");
        }
    }

    private Mono<Boolean> createAndSeed(ReactiveIndexOperations indexOps) {
        return indexOps.createWithMapping()
                .then(operations.withRefreshPolicy(RefreshPolicy.IMMEDIATE)
                        .saveAll(SampleProducts.all(), Product.class)
                        .then())
                .thenReturn(true);
    }
}
