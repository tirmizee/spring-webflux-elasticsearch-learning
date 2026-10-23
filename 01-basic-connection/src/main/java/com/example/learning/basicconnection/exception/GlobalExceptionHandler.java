package com.example.learning.basicconnection.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralized exception handling for the whole application.
 *
 * <p>Maps unexpected errors (e.g. Elasticsearch unreachable) to a 503 response. The message is
 * intentionally generic and never includes connection details or credentials, so error responses
 * cannot leak sensitive configuration.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problem.setTitle("Elasticsearch request failed");
        problem.setDetail("Could not complete the request against Elasticsearch. "
                + "Check that the cluster is running and reachable.");
        return problem;
    }
}
