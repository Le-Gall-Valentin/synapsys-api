package com.synapsys.api.agent.infrastructure.web.dto;

import com.synapsys.api.agent.domain.model.DerivedAgentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Agent enregistré")
public record AgentResponse(
    UUID id, String serverName, String ipAddress, DerivedAgentStatus status,
    String fingerprint, Instant enrolledAt, Instant lastActivityAt
) {}
