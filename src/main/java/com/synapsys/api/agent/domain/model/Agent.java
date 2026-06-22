package com.synapsys.api.agent.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Agent(
    UUID id,
    String serverName,
    byte[] publicKey,
    String fingerprint,
    AgentLifecycleStatus status,
    UUID enrollmentTokenId,
    Instant enrolledAt,
    UUID enrolledBy,
    Instant firstConnectedAt,
    Instant lastActivityAt,
    String ipAddress,
    Instant revokedAt,
    UUID revokedBy
) {
    public DerivedAgentStatus deriveStatus(boolean present) {
        if (status == AgentLifecycleStatus.REVOKED) return DerivedAgentStatus.REVOKED;
        if (present) return DerivedAgentStatus.ACTIVE;
        if (firstConnectedAt == null) return DerivedAgentStatus.PENDING;
        return DerivedAgentStatus.INACTIVE;
    }

    public void ensureRevocable() {
        if (status == AgentLifecycleStatus.REVOKED) throw new AgentException.AgentNotRevocable();
    }

    public void ensureDeletable() {
        if (status != AgentLifecycleStatus.REVOKED) throw new AgentException.AgentNotDeletable();
    }

    public void ensureConnectable() {
        if (status == AgentLifecycleStatus.REVOKED) throw new AgentException.HandshakeFailed();
    }
}