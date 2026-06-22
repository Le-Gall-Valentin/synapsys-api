package com.synapsys.api.agent.domain.model;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentToken(
    UUID id,
    String serverName,
    Instant consumedAt,
    Instant revokedAt,
    UUID revokedBy,
    Instant expiresAt,
    Instant createdAt,
    UUID createdBy
) {
    public EnrollmentTokenStatus deriveStatus(Instant now) {
        if (revokedAt != null) return EnrollmentTokenStatus.REVOKED;
        if (consumedAt != null) return EnrollmentTokenStatus.CONSUMED;
        if (!now.isBefore(expiresAt)) return EnrollmentTokenStatus.EXPIRED;
        return EnrollmentTokenStatus.ACTIVE;
    }

    public void ensureConsumable(Instant now) {
        if (deriveStatus(now) != EnrollmentTokenStatus.ACTIVE) {
            throw new AgentException.TokenNotConsumable();
        }
    }

    public void ensureRevocable(Instant now) {
        if (deriveStatus(now) != EnrollmentTokenStatus.ACTIVE) {
            throw new AgentException.TokenNotRevocable();
        }
    }
}
