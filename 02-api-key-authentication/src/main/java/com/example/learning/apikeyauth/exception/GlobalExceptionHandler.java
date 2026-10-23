package com.example.learning.apikeyauth.exception;

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
 * Centralized exception handling. Extends Chapter 01's classification with the two cases that
 * matter for API key security, clearly <strong>distinguishing authentication from authorization</strong>:
 *
 * <ul>
 *     <li><b>401 Unauthorized</b> — <em>authentication</em> failed: the API key is missing, invalid,
 *         malformed, or has been invalidated/revoked. Elasticsearch replies with HTTP 401.</li>
 *     <li><b>403 Forbidden</b> — <em>authorization</em> failed: the API key is valid but lacks the
 *         privileges for the requested action (least-privilege). Elasticsearch replies with HTTP 403.</li>
 *     <li><b>504 Gateway Timeout</b> — the request to Elasticsearch timed out.</li>
 *     <li><b>503 Service Unavailable</b> — Elasticsearch is unreachable.</li>
 *     <li><b>502 Bad Gateway</b> — Elasticsearch returned some other error response.</li>
 *     <li><b>500 Internal Server Error</b> — an unexpected application error.</li>
 * </ul>
 *
 * <p>Response bodies are generic {@link ProblemDetail}s. They never include the API key, the
 * {@code Authorization} header, hosts, URIs, stack traces, or raw Elasticsearch messages, so no
 * credentials or infrastructure details can leak. Full details are logged server-side only (and
 * the logger never prints the key).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;

    /**
     * Elasticsearch responded with an error status. We branch on the HTTP status so that
     * authentication (401) and authorization (403) failures are reported distinctly, and other
     * error responses fall back to 502.
     */
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

    /**
     * Fallback. Some failures surface wrapped (e.g. by the reactive transport), so the cause chain
     * is inspected: a wrapped ES error (401/403/other) is classified first, then timeout, then
     * connectivity; anything else is an unexpected application error (500).
     */
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
