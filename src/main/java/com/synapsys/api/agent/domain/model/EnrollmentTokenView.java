package com.synapsys.api.agent.domain.model;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentTokenView(
    UUID id, String serverName, EnrollmentTokenStatus status, Instant expiresAt, UUID createdBy, Instant createdAt) {}