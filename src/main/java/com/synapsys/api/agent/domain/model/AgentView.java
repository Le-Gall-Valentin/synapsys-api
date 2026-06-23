package com.synapsys.api.agent.domain.model;

import java.time.Instant;
import java.util.UUID;

public record AgentView(
    UUID id, String serverName, String ipAddress, DerivedAgentStatus status,
    String fingerprint, Instant enrolledAt, Instant lastActivityAt) {}
