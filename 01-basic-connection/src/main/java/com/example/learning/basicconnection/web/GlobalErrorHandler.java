package com.example.learning.basicconnection.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps unexpected errors (e.g. Elasticsearch unreachable) to a 503 response.
 *
 * <p>The message is intentionally generic and never includes connection details or
 * credentials, so error responses cannot leak sensitive configuration.
 */
@RestControllerAdvice
public class GlobalErrorHandler {

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problem.setTitle("Elasticsearch request failed");
        problem.setDetail("Could not complete the request against Elasticsearch. "
                + "Check that the cluster is running and reachable.");
        return problem;
    }
}
