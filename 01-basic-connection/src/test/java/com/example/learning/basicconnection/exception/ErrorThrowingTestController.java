package com.example.learning.basicconnection.exception;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.ConnectException;

/**
 * Test-only controller that raises each category of exception so {@link GlobalExceptionHandler}
 * classification can be verified in a WebFlux slice. Lives in the test source tree only.
 */
@RestController
public class ErrorThrowingTestController {

    @GetMapping("/throw/connect")
    Mono<String> connect() {
        return Mono.error(new ConnectException("Connection refused: localhost/127.0.0.1:9200"));
    }

    @GetMapping("/throw/io")
    Mono<String> io() {
        return Mono.error(new IOException("socket closed to es-node-1.internal:9200"));
    }

    @GetMapping("/throw/es")
    Mono<String> es() {
        ErrorResponse errorResponse = ErrorResponse.of(r -> r
                .status(500)
                .error(e -> e.type("search_phase_execution_exception").reason("all shards failed")));
        return Mono.error(new ElasticsearchException("es/search", errorResponse));
    }

    @GetMapping("/throw/wrapped-connect")
    Mono<String> wrappedConnect() {
        return Mono.error(new RuntimeException("transport failure",
                new ConnectException("Connection refused: 10.0.0.5:9200")));
    }

    @GetMapping("/throw/bug")
    Mono<String> bug() {
        return Mono.error(new IllegalStateException("null pointer in mapper at line 42"));
    }
}
