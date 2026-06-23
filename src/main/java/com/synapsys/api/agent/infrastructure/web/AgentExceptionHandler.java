package com.synapsys.api.agent.infrastructure.web;

import com.synapsys.api.agent.domain.model.AgentException;
import com.synapsys.api.shared.infrastructure.web.ProblemDetailFactory;
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
public class AgentExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AgentExceptionHandler.class);

    @ExceptionHandler(AgentException.class)
    public ResponseEntity<ProblemDetail> handle(AgentException e, HttpServletRequest request) {
        AgentErrorResponse r = switch (e) {
            case AgentException.TokenNotFound ex -> response(404, ex, "Enrollment token not found.");
            case AgentException.TokenNotRevocable ex -> response(409, ex, "Enrollment token cannot be revoked.");
            case AgentException.InvalidPublicKey ex -> response(400, ex, "Public key is not a valid Ed25519 key.");
            case AgentException.PublicKeyAlreadyRegistered ex -> response(409, ex, "Public key is already registered.");
            case AgentException.ServerNameInUse ex -> response(409, ex, "Server name is already in use.");
            case AgentException.AgentNotFound ex -> response(404, ex, "Agent not found.");
            case AgentException.AgentNotRevocable ex -> response(409, ex, "Agent cannot be revoked.");
            case AgentException.AgentNotDeletable ex -> response(409, ex, "Agent must be revoked before deletion.");
            case AgentException.HandshakeFailed ex -> response(401, ex, "Agent handshake failed.");
            case AgentException.DataIntegrityError ex -> {
                log.error("Data integrity violation on {}", request.getRequestURI(), ex);
                yield response(500, ex, "An unexpected error occurred. Please try again later.");
            }
            case AgentException.EnrollmentRejected ex -> response(422, ex, "Enrollment rejected.");
        };
        ProblemDetail problem = ProblemDetailFactory.of(
            HttpStatus.valueOf(r.status()), r.title(), r.detail(), URI.create(request.getRequestURI()));
        return ResponseEntity.status(r.status()).body(problem);
    }

    private static AgentErrorResponse response(int status, AgentException e, String detail) {
        return new AgentErrorResponse(status, e.getClass().getSimpleName(), detail);
    }

    private record AgentErrorResponse(int status, String title, String detail) {}
}
