package com.synapsys.api.identity.infrastructure.web;

import com.synapsys.api.identity.domain.model.IdentityException;
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
public class IdentityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(IdentityExceptionHandler.class);

    @ExceptionHandler(IdentityException.class)
    public ResponseEntity<ProblemDetail> handle(IdentityException e, HttpServletRequest request) {
        IdentityErrorResponse response = switch (e) {
            case IdentityException.UserNotFound ex ->
                    response(404, ex, ex.getMessage());

            case IdentityException.UserNotActive ex ->
                    response(403, ex, "User account is not active.");

            case IdentityException.InsufficientPermissions ex ->
                    response(403, ex, "Insufficient permissions");

            case IdentityException.UserAlreadyInactive ex ->
                    response(409, ex, ex.getMessage());

            case IdentityException.UsernameAlreadyExists ex ->
                    response(409, ex, ex.getMessage());

            case IdentityException.EmailAlreadyExists ex ->
                    response(409, ex, ex.getMessage());

            case IdentityException.DataIntegrityError ex -> {
                log.error("Data integrity violation on {}", request.getRequestURI(), ex);
                yield response(500, ex, "An unexpected error occurred. Please try again later.");
            }
        };

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.valueOf(response.status()),
                response.detail()
        );
        problem.setTitle(response.title());
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(response.status()).body(problem);
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

    private static IdentityErrorResponse response(int status, IdentityException e, String detail) {
        return new IdentityErrorResponse(status, e.getClass().getSimpleName(), detail);
    }

    private record IdentityErrorResponse(int status, String title, String detail) {}
}