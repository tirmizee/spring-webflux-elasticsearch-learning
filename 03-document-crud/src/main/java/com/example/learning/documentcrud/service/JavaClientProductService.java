package com.example.learning.documentcrud.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.OpType;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.GetResponse;
import com.example.learning.documentcrud.domain.Product;
import com.example.learning.documentcrud.dto.ProductRequest;
import com.example.learning.documentcrud.dto.ProductResponse;
import com.example.learning.documentcrud.exception.DuplicateProductException;
import com.example.learning.documentcrud.exception.ProductNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;

/**
 * Product CRUD via the official Elasticsearch Java API Client ({@link ElasticsearchClient}).
 *
 * <p>The client is <strong>synchronous</strong>; every call is wrapped in
 * {@link Mono#fromCallable} and moved onto {@link Schedulers#boundedElastic()} so blocking I/O
 * never runs on the reactor-netty event loop. {@code Mono.just(blockingOp())} is intentionally
 * NOT used, since that would run the blocking call eagerly on the subscribing thread.
 *
 * <p>Writes use {@code refresh=true} for immediate read-after-write visibility (learning context),
 * and create uses {@code op_type=create} so Elasticsearch itself rejects a duplicate id with 409;
 * that is translated into {@link DuplicateProductException}.
 */
@Service
public class JavaClientProductService {

    private static final String CLIENT = "java-client";
    private static final String INDEX = "learning-products";
    private static final int HTTP_CONFLICT = 409;

    private final ElasticsearchClient client;

    public JavaClientProductService(ElasticsearchClient client) {
        this.client = client;
    }

    public Mono<ProductResponse> create(String id, ProductRequest request) {
        return Mono.fromCallable(() -> blockingCreate(id, request))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<ProductResponse> get(String id) {
        return Mono.fromCallable(() -> blockingGet(id))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<ProductResponse> update(String id, ProductRequest request) {
        return Mono.fromCallable(() -> blockingUpdate(id, request))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> delete(String id) {
        return Mono.fromCallable(() -> {
            blockingDelete(id);
            return (Void) null;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private ProductResponse blockingCreate(String id, ProductRequest request) throws IOException {
        Product product = toProduct(id, request);
        try {
            client.index(i -> i
                    .index(INDEX)
                    .id(id)
                    .opType(OpType.Create)   // fail if the id already exists
                    .refresh(Refresh.True)
                    .document(product));
        } catch (ElasticsearchException ex) {
            if (ex.status() == HTTP_CONFLICT) {
                throw new DuplicateProductException(id);
            }
            throw ex;
        }
        return ProductResponse.of(CLIENT, product);
    }

    private ProductResponse blockingGet(String id) throws IOException {
        GetResponse<Product> response = client.get(g -> g.index(INDEX).id(id), Product.class);
        if (!response.found() || response.source() == null) {
            throw new ProductNotFoundException(id);
        }
        Product source = response.source();
        source.setId(id);
        return ProductResponse.of(CLIENT, source);
    }

    private ProductResponse blockingUpdate(String id, ProductRequest request) throws IOException {
        // Full replacement of an existing document; 404 if it does not exist (no upsert).
        boolean exists = client.exists(e -> e.index(INDEX).id(id)).value();
        if (!exists) {
            throw new ProductNotFoundException(id);
        }
        Product product = toProduct(id, request);
        client.index(i -> i
                .index(INDEX)
                .id(id)
                .opType(OpType.Index)    // replace the whole document
                .refresh(Refresh.True)
                .document(product));
        return ProductResponse.of(CLIENT, product);
    }

    private void blockingDelete(String id) throws IOException {
        boolean exists = client.exists(e -> e.index(INDEX).id(id)).value();
        if (!exists) {
            throw new ProductNotFoundException(id);
        }
        client.delete(d -> d.index(INDEX).id(id).refresh(Refresh.True));
    }

    private Product toProduct(String id, ProductRequest r) {
        return new Product(id, r.name(), r.description(), r.price(), r.quantity());
    }
}
