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
        AuthErrorResponse response = switch (e) {
            case AuthException.InvalidCredentials ex ->
                    response(401, ex, ex.getMessage());

            case AuthException.UserNotActive ignored ->
                    invalidCredentialsResponse();

            case AuthException.UserNotFound ignored ->
                    invalidCredentialsResponse();

            case AuthException.TokenExpired ex ->
                    response(401, ex, "Authentication required");

            case AuthException.TokenRevoked ex ->
                    response(401, ex, "Authentication required");

            case AuthException.UsernameAlreadyExists ex ->
                    response(409, ex, ex.getMessage());

            case AuthException.EmailAlreadyExists ex ->
                    response(409, ex, ex.getMessage());

            case AuthException.InsufficientPermissions ex ->
                    response(403, ex, "Insufficient permissions");

            case AuthException.DataIntegrityError ex -> {
                log.error("Data integrity violation on {}", request.getRequestURI());

                yield response(
                        500,
                        ex,
                        "An unexpected error occurred. Please try again later."
                );
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

    private static AuthErrorResponse invalidCredentialsResponse() {
        return new AuthErrorResponse(
                401,
                AuthException.InvalidCredentials.class.getSimpleName(),
                "Invalid credentials"
        );
    }


    private static AuthErrorResponse response(int status, AuthException e, String detail) {
        return new AuthErrorResponse(
                status,
                e.getClass().getSimpleName(),
                detail
        );
    }

    private record AuthErrorResponse(
            int status,
            String title,
            String detail
    ) {}
}