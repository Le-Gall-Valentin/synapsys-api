package com.synapsys.api.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RefreshToken(
    UUID id,
    UUID userId,
    String tokenHash,
    Instant expiresAt,
    boolean revoked,
    Instant createdAt,
    Instant lastUsedAt
) {
    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    public boolean isRevoked() {
        return revoked;
    }
}