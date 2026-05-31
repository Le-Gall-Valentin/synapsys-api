package com.synapsys.api.authentication.infrastructure.web;

import com.synapsys.api.authentication.domain.model.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class AuthenticationExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handle(AuthenticationException e, HttpServletRequest request) {
        AuthErrorResponse response = switch (e) {
            case AuthenticationException.InvalidCredentials ex ->
                    response(401, ex, ex.getMessage());

            case AuthenticationException.UserNotActive ex ->
                    response(403, ex, "User account is not active.");

            case AuthenticationException.UserNotFound ex ->
                    response(404, ex, "User not found");

            case AuthenticationException.TokenExpired ex ->
                    response(401, ex, "Authentication required");

            case AuthenticationException.TokenNotFound ex ->
                    response(401, ex, "Authentication required");

            case AuthenticationException.TokenRevoked ex ->
                    response(401, ex, "Authentication required");

            case AuthenticationException.TotpCodeInvalid ex ->
                    response(401, ex, "Authentication required");

            case AuthenticationException.TotpChallengeExpired ex ->
                    response(401, ex, "Authentication required");

            case AuthenticationException.TotpMaxAttemptsExceeded ex ->
                    response(401, ex, "Authentication required");
        };

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.valueOf(response.status()),
                response.detail()
        );

        problem.setTitle(response.title());
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(response.status()).body(problem);
    }

    private static AuthErrorResponse response(int status, AuthenticationException e, String detail) {
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