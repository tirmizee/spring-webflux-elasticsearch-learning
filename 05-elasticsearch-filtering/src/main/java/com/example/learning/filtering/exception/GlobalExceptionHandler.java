package com.example.learning.filtering.exception;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

/**
 * Centralized exception handling, consistent with Chapters 02-04.
 *
 * <ul>
 *     <li><b>400</b> — request validation failure / malformed body / invalid filter semantics</li>
 *     <li><b>401 / 403</b> — Elasticsearch authentication / authorization failure (API key)</li>
 *     <li><b>504 / 503 / 502</b> — Elasticsearch timeout / unreachable / other upstream error</li>
 *     <li><b>500</b> — unexpected application error</li>
 * </ul>
 *
 * <p>Note: an empty filter result is a normal HTTP 200 with an empty list, handled in the service
 * layer, so there is deliberately no 404 mapping for "no hits". Response bodies are generic
 * {@link ProblemDetail}s that never leak the API key, hosts, URIs, stack traces, or raw ES messages.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;

    @ExceptionHandler({WebExchangeBindException.class, ServerWebInputException.class})
    public ProblemDetail handleBadRequest(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid request");
        if (ex instanceof WebExchangeBindException bind && bind.getFieldError() != null) {
            problem.setDetail(bind.getFieldError().getField() + ": "
                    + bind.getFieldError().getDefaultMessage());
        } else {
            problem.setDetail("The request is missing required parameters or has an invalid body.");
        }
        return problem;
    }

    @ExceptionHandler(InvalidFilterException.class)
    public ProblemDetail handleInvalidFilter(InvalidFilterException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid filter");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(ElasticsearchException.class)
    public ProblemDetail handleElasticsearchResponse(ElasticsearchException ex) {
        int status = ex.status();
        if (status == HTTP_UNAUTHORIZED) {
            log.warn("Elasticsearch authentication failed (HTTP 401 from ES)");
            return authenticationFailed();
        }
        if (status == HTTP_FORBIDDEN) {
            log.warn("Elasticsearch authorization failed (HTTP 403 from ES)");
            return authorizationFailed();
        }
        log.warn("Elasticsearch returned an error response (status={})", status);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        problem.setTitle("Elasticsearch returned an error");
        problem.setDetail("Elasticsearch was reached but returned an error response.");
        return problem;
    }

    @ExceptionHandler({SocketTimeoutException.class, TimeoutException.class})
    public ProblemDetail handleTimeout(Exception ex) {
        log.warn("Elasticsearch request timed out: {}", ex.getClass().getSimpleName());
        return gatewayTimeout();
    }

    @ExceptionHandler({ConnectException.class, UnknownHostException.class, IOException.class})
    public ProblemDetail handleUnavailable(Exception ex) {
        log.warn("Elasticsearch is unavailable: {}", ex.getClass().getSimpleName());
        return unavailable();
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        ElasticsearchException esError = findCause(ex, ElasticsearchException.class);
        if (esError != null) {
            return handleElasticsearchResponse(esError);
        }
        if (containsCause(ex, SocketTimeoutException.class, TimeoutException.class)) {
            log.warn("Elasticsearch request timed out (wrapped): {}", ex.getClass().getSimpleName());
            return gatewayTimeout();
        }
        if (containsCause(ex, ConnectException.class, UnknownHostException.class, IOException.class)) {
            log.warn("Elasticsearch is unavailable (wrapped): {}", ex.getClass().getSimpleName());
            return unavailable();
        }
        log.error("Unexpected application error", ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Internal server error");
        problem.setDetail("An unexpected error occurred while processing the request.");
        return problem;
    }

    private ProblemDetail authenticationFailed() {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Elasticsearch authentication failed");
        problem.setDetail("The Elasticsearch API key is missing, invalid, or has been revoked.");
        return problem;
    }

    private ProblemDetail authorizationFailed() {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Elasticsearch authorization failed");
        problem.setDetail("The API key is valid but does not have sufficient privileges for this action.");
        return problem;
    }

    private ProblemDetail unavailable() {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problem.setTitle("Elasticsearch unavailable");
        problem.setDetail("Could not reach Elasticsearch. Check that the cluster is running and reachable.");
        return problem;
    }

    private ProblemDetail gatewayTimeout() {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.GATEWAY_TIMEOUT);
        problem.setTitle("Elasticsearch timeout");
        problem.setDetail("The request to Elasticsearch timed out. Please try again later.");
        return problem;
    }

    @SafeVarargs
    private boolean containsCause(Throwable ex, Class<? extends Throwable>... types) {
        Throwable current = ex;
        for (int depth = 0; current != null && depth < 10; depth++) {
            for (Class<? extends Throwable> type : types) {
                if (type.isInstance(current)) {
                    return true;
                }
            }
            if (current == current.getCause()) {
                break;
            }
            current = current.getCause();
        }
        return false;
    }

    private <T extends Throwable> T findCause(Throwable ex, Class<T> type) {
        Throwable current = ex;
        for (int depth = 0; current != null && depth < 10; depth++) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            if (current == current.getCause()) {
                break;
            }
            current = current.getCause();
        }
        return null;
    }
}
