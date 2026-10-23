package com.example.learning.apikeyauth.exception;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

/**
 * Test-only controller raising each category of exception so {@link GlobalExceptionHandler}
 * classification can be verified in a WebFlux slice.
 */
@RestController
public class ErrorThrowingTestController {

    private static ElasticsearchException esError(int status, String type, String reason) {
        ErrorResponse response = ErrorResponse.of(r -> r
                .status(status)
                .error(e -> e.type(type).reason(reason)));
        return new ElasticsearchException("es/endpoint", response);
    }

    @GetMapping("/throw/unauthorized")
    Mono<String> unauthorized() {
        // 401 from ES: missing / invalid / revoked API key (authentication failure).
        return Mono.error(esError(401, "security_exception",
                "unable to authenticate with provided credentials and anonymous access is not allowed"));
    }

    @GetMapping("/throw/forbidden")
    Mono<String> forbidden() {
        // 403 from ES: valid key but insufficient privileges (authorization failure).
        return Mono.error(esError(403, "security_exception",
                "action [cluster:monitor/health] is unauthorized for API key id [abc123]"));
    }

    @GetMapping("/throw/es-other")
    Mono<String> esOther() {
        return Mono.error(esError(400, "illegal_argument_exception", "something invalid"));
    }

    @GetMapping("/throw/wrapped-unauthorized")
    Mono<String> wrappedUnauthorized() {
        return Mono.error(new RuntimeException("transport layer",
                esError(401, "security_exception", "invalid ApiKey header for key id [xyz]")));
    }

    @GetMapping("/throw/connect")
    Mono<String> connect() {
        return Mono.error(new ConnectException("Connection refused: localhost/127.0.0.1:9200"));
    }

    @GetMapping("/throw/timeout")
    Mono<String> timeout() {
        return Mono.error(new SocketTimeoutException("Read timed out to es-node-1:9200"));
    }

    @GetMapping("/throw/io")
    Mono<String> io() {
        return Mono.error(new IOException("socket closed to es-node-1:9200"));
    }

    @GetMapping("/throw/bug")
    Mono<String> bug() {
        return Mono.error(new IllegalStateException("null pointer in mapper at line 42"));
    }
}
