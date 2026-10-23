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
import java.net.UnknownHostException;

/**
 * Centralized exception handling for the whole application.
 *
 * <p>Errors are <strong>classified</strong> rather than all mapped to the same status:
 * <ul>
 *     <li><b>503 Service Unavailable</b> — Elasticsearch is unreachable/unavailable
 *         (connection refused, unknown host, I/O / transport failure). Retrying later may help.</li>
 *     <li><b>502 Bad Gateway</b> — Elasticsearch was reached but returned an error response
 *         ({@link ElasticsearchException}). The upstream, not this service, rejected the request.</li>
 *     <li><b>500 Internal Server Error</b> — an unexpected application error that is not an
 *         Elasticsearch connectivity or response problem.</li>
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
     * Connectivity/availability failures: the node could not be reached at all.
     * {@code TransportException} is an {@link IOException}, so it is covered here too.
     */
    @ExceptionHandler({ConnectException.class, UnknownHostException.class, IOException.class})
    public ProblemDetail handleUnavailable(Exception ex) {
        log.warn("Elasticsearch is unavailable: {}", ex.getClass().getSimpleName(), ex);
        return unavailable();
    }

    /**
     * Fallback. Some connectivity failures surface wrapped in a {@link RuntimeException}
     * (e.g. by the reactive transport), so we inspect the cause chain: if it is a connectivity
     * problem we still return 503, otherwise it is an unexpected application error → 500.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        if (isConnectivityFailure(ex)) {
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

    /**
     * Walks the cause chain looking for a known connectivity-failure type. Bounded to avoid
     * pathological or cyclic cause chains.
     */
    private boolean isConnectivityFailure(Throwable ex) {
        Throwable current = ex;
        for (int depth = 0; current != null && depth < 10; depth++) {
            if (current instanceof ConnectException
                    || current instanceof UnknownHostException
                    || current instanceof IOException) {
                return true;
            }
            if (current == current.getCause()) {
                break;
            }
            current = current.getCause();
        }
        return false;
    }
}
