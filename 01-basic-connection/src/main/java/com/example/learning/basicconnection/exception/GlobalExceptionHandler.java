package com.example.learning.basicconnection.exception;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

/**
 * Centralized exception handling for the whole application.
 *
 * <p>Errors are <strong>classified</strong> rather than all mapped to the same status:
 * <ul>
 *     <li><b>504 Gateway Timeout</b> — the request to Elasticsearch timed out
 *         ({@link SocketTimeoutException}, reactive {@link TimeoutException}). The node may be
 *         reachable but too slow to answer within the configured timeout.</li>
 *     <li><b>503 Service Unavailable</b> — Elasticsearch is unreachable/unavailable
 *         (connection refused, unknown host, I/O / transport failure). Retrying later may help.</li>
 *     <li><b>502 Bad Gateway</b> — Elasticsearch was reached but returned an error response
 *         ({@link ElasticsearchException}). The upstream, not this service, rejected the request.</li>
 *     <li><b>500 Internal Server Error</b> — an unexpected application error that is not an
 *         Elasticsearch connectivity, timeout, or response problem.</li>
 * </ul>
 *
 * <p>In every case the response body is a generic {@link ProblemDetail} that never includes
 * hosts, URIs, credentials, stack traces, or raw vendor messages, so infrastructure details
 * cannot leak. Full details are logged server-side only.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Elasticsearch responded with an error status (server reachable, request rejected/failed
     * upstream). Treated as a bad gateway: the problem is upstream, not the client's request.
     */
    @ExceptionHandler(ElasticsearchException.class)
    public ProblemDetail handleElasticsearchResponse(ElasticsearchException ex) {
        log.warn("Elasticsearch returned an error response (status={})", ex.status(), ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        problem.setTitle("Elasticsearch returned an error");
        problem.setDetail("Elasticsearch was reached but returned an error response.");
        return problem;
    }

    /**
     * Request timed out talking to Elasticsearch. More specific than {@link IOException}, so
     * {@link SocketTimeoutException} is classified here (504) rather than as unavailable (503).
     * {@link TimeoutException} comes from reactive timeout operators.
     */
    @ExceptionHandler({SocketTimeoutException.class, TimeoutException.class})
    public ProblemDetail handleTimeout(Exception ex) {
        log.warn("Elasticsearch request timed out: {}", ex.getClass().getSimpleName(), ex);
        return gatewayTimeout();
    }

    /**
     * Connectivity/availability failures: the node could not be reached at all.
     * {@code TransportException} is an {@link IOException}, so it is covered here too.
     */
    @ExceptionHandler({ConnectException.class, UnknownHostException.class, IOException.class})
    public ProblemDetail handleUnavailable(Exception ex) {
        log.warn("Elasticsearch is unavailable: {}", ex.getClass().getSimpleName(), ex);
        return unavailable();
    }

    /**
     * Fallback. Some failures surface wrapped in another exception (e.g. by the reactive
     * transport), so we inspect the cause chain: a wrapped timeout → 504, a wrapped connectivity
     * problem → 503, otherwise an unexpected application error → 500.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        if (containsCause(ex, SocketTimeoutException.class, TimeoutException.class)) {
            log.warn("Elasticsearch request timed out (wrapped): {}", ex.getClass().getSimpleName(), ex);
            return gatewayTimeout();
        }
        if (containsCause(ex, ConnectException.class, UnknownHostException.class, IOException.class)) {
            log.warn("Elasticsearch is unavailable (wrapped): {}", ex.getClass().getSimpleName(), ex);
            return unavailable();
        }
        log.error("Unexpected application error", ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Internal server error");
        problem.setDetail("An unexpected error occurred while processing the request.");
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

    /**
     * Walks the cause chain (bounded to avoid pathological/cyclic chains) looking for any of the
     * given exception types. Timeouts are checked before connectivity because
     * {@link SocketTimeoutException} is itself an {@link IOException}.
     */
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
}
