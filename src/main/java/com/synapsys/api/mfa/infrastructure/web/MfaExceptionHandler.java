package com.synapsys.api.mfa.infrastructure.web;

import com.synapsys.api.mfa.domain.model.MfaException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.net.URI;

@RestControllerAdvice
public class MfaExceptionHandler {

    @ExceptionHandler(MfaException.class)
    public ResponseEntity<ProblemDetail> handle(MfaException e, HttpServletRequest request) {
        int status = switch (e) {
            case MfaException.UserNotFound ex                     -> 404;
            case MfaException.TotpAlreadyEnabled ex               -> 409;
            case MfaException.TotpNotEnabled ex                   -> 409;
            case MfaException.TotpSetupNotStarted ex              -> 422;
            case MfaException.TotpCodeInvalid ex                  -> 401;
            case MfaException.TotpConfirmMaxAttemptsExceeded ex   -> 429;
            case MfaException.InsufficientPermissions ex          -> 403;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), e.getMessage());
        problem.setTitle(e.getClass().getSimpleName());
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(status).body(problem);
    }
}