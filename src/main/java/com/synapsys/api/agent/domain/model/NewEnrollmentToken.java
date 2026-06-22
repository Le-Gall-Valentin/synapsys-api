package com.synapsys.api.agent.domain.model;

import java.time.Instant;
import java.util.UUID;

public record NewEnrollmentToken(String serverName, String tokenHash, Instant expiresAt, UUID createdBy) {}