package com.synapsys.api.agent.infrastructure.web.dto;

import com.synapsys.api.agent.domain.model.EnrollmentTokenStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Token d'enrôlement (sans le secret)")
public record EnrollmentTokenResponse(
    UUID id,
    String serverName,
    EnrollmentTokenStatus status,
    Instant expiresAt,
    UUID createdBy,
    Instant createdAt
) {}
