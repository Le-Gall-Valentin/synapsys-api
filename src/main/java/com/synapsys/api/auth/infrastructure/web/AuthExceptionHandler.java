package com.synapsys.api.auth.infrastructure.web;

import com.synapsys.api.auth.domain.model.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class AuthExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthExceptionHandler.class);

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ProblemDetail> handle(AuthException e, HttpServletRequest request) {
        int status = switch (e) {
            case AuthException.InvalidCredentials      __ -> 401;
            case AuthException.UserNotActive           __ -> 403;
            case AuthException.TokenExpired            __ -> 401;
            case AuthException.TokenRevoked            __ -> 401;
            case AuthException.UserNotFound            __ -> 404;
            case AuthException.UsernameAlreadyExists   __ -> 409;
            case AuthException.EmailAlreadyExists      __ -> 409;
            case AuthException.InsufficientPermissions __ -> 403;
            case AuthException.DataIntegrityError      __ -> 500;
        };

        String detail;
        if (e instanceof AuthException.DataIntegrityError) {
            log.error("Data integrity violation on {}: {}", request.getRequestURI(), e.getMessage());
            detail = "An unexpected error occurred. Please try again later.";
        } else {
            detail = e.getMessage();
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), detail);
        problem.setTitle(e.getClass().getSimpleName());
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException e,
                                                          HttpServletRequest request) {
        String details = e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining(", "));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, details);
        problem.setTitle("Validation failed");
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.badRequest().body(problem);
    }
}