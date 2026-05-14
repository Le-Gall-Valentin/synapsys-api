package com.synapsys.api.auth.infrastructure.web;

import com.synapsys.api.auth.domain.model.AuthException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Void> handle(AuthException e) {
        int status = switch (e) {
            case AuthException.InvalidCredentials __ -> 401;
            case AuthException.UserNotActive      __ -> 403;
            case AuthException.TokenExpired       __ -> 401;
            case AuthException.TokenRevoked       __ -> 401;
            case AuthException.UserNotFound       __ -> 404;
        };
        return ResponseEntity.status(status).build();
    }
}