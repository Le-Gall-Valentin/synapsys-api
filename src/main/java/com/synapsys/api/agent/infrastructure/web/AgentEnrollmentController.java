package com.synapsys.api.agent.infrastructure.web;

import com.synapsys.api.agent.application.dto.EnrollmentResult;
import com.synapsys.api.agent.application.port.in.EnrollAgentUseCase;
import com.synapsys.api.agent.domain.model.AgentException;
import com.synapsys.api.agent.domain.model.EnrollAgentCommand;
import com.synapsys.api.agent.infrastructure.config.AgentProperties;
import com.synapsys.api.agent.infrastructure.web.dto.EnrollAgentRequest;
import com.synapsys.api.agent.infrastructure.web.dto.EnrollAgentResponse;
import com.synapsys.api.infrastructure.ratelimit.RateLimiting;
import com.synapsys.api.infrastructure.ratelimit.RateLimitMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;

@Tag(name = "Agents - Enrôlement")
@RestController
@RequestMapping("/api/agents")
public class AgentEnrollmentController {

    private final EnrollAgentUseCase enrollUseCase;
    private final AgentProperties properties;

    public AgentEnrollmentController(EnrollAgentUseCase enrollUseCase, AgentProperties properties) {
        this.enrollUseCase = enrollUseCase;
        this.properties = properties;
    }

    @Operation(summary = "Enrôler un agent",
        description = "Endpoint agent (non soumis aux rôles). Consomme un token d'enrôlement à usage unique et enregistre la clé publique Ed25519. Rate limit : 5 req/min par IP.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Agent enrôlé (statut initial PENDING)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = EnrollAgentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Clé publique invalide (mauvais format / longueur)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Clé publique déjà enregistrée ou nom de serveur déjà utilisé",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Enrôlement rejeté (token inconnu, expiré, consommé ou révoqué)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "429", description = "Trop de requêtes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/enroll")
    @RateLimiting(mode = RateLimitMode.IP, max = 5, windowSeconds = 60)
    public ResponseEntity<EnrollAgentResponse> enroll(@Valid @RequestBody EnrollAgentRequest request) {
        byte[] publicKey = decodePublicKey(request.publicKey());
        EnrollmentResult result = enrollUseCase.enroll(new EnrollAgentCommand(request.token(), publicKey));
        return ResponseEntity.status(201).body(new EnrollAgentResponse(
            result.agentId(), result.serverName(), result.fingerprint(), properties.websocketPath()));
    }

    private static byte[] decodePublicKey(String base64Url) {
        try {
            return Base64.getUrlDecoder().decode(base64Url);
        } catch (IllegalArgumentException e) {
            throw new AgentException.InvalidPublicKey();
        }
    }
}
