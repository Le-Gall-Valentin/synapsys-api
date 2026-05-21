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
            case AuthException.InvalidCredentials ignored -> 401;
            case AuthException.UserNotActive ignored -> 401;
            case AuthException.TokenExpired ignored -> 401;
            case AuthException.TokenRevoked ignored -> 401;
            case AuthException.UserNotFound ignored -> 401;
            case AuthException.UsernameAlreadyExists ignored -> 409;
            case AuthException.EmailAlreadyExists ignored -> 409;
            case AuthException.InsufficientPermissions ignored -> 403;
            case AuthException.DataIntegrityError ignored -> 500;
        };

        String title = e.getClass().getSimpleName();
        String detail;
        if (e instanceof AuthException.DataIntegrityError) {
            log.error("Data integrity violation on {}: {}", request.getRequestURI(), e.getMessage());
            detail = "An unexpected error occurred. Please try again later.";
        } else if (e instanceof AuthException.UserNotActive || e instanceof AuthException.UserNotFound) {
            // Mask account state/existence: return same response as InvalidCredentials to prevent enumeration
            title = AuthException.InvalidCredentials.class.getSimpleName();
            detail = "Invalid credentials";
        } else {
            detail = e.getMessage();
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), detail);
        problem.setTitle(title);
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