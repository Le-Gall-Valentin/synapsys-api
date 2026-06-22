package com.synapsys.api.agent.infrastructure.web.dto;

import com.synapsys.api.agent.domain.model.EnrollmentTokenStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Token d'enrôlement créé. Le champ `token` n'est renvoyé qu'à la création.")
public record CreatedTokenResponse(
    UUID id,
    String serverName,
    @Schema(description = "Token en clair - à copier maintenant, non récupérable ensuite") String token,
    EnrollmentTokenStatus status,
    Instant expiresAt,
    Instant createdAt
) {}
