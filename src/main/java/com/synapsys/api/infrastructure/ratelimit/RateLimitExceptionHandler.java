package com.synapsys.api.infrastructure.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.http.HttpHeaders;

import java.net.URI;

@RestControllerAdvice
public class RateLimitExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handle(RateLimitExceededException e, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
        problem.setTitle("RateLimitExceeded");
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(429)
            .header(HttpHeaders.RETRY_AFTER, "60")
            .body(problem);
    }
}