package com.synapsys.api.agent.domain.model;

import java.time.Instant;
import java.util.UUID;

public record IssuedToken(UUID id, String rawToken, String serverName, Instant expiresAt, Instant createdAt) {}
