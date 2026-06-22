package com.synapsys.api.agent.infrastructure.persistence.entity;

import com.synapsys.api.agent.domain.model.AgentLifecycleStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "agents",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_agents_public_key", columnNames = "public_key"),
        @UniqueConstraint(name = "uq_agents_fingerprint", columnNames = "fingerprint")
    },
    indexes = @Index(name = "idx_agents_status", columnList = "status")
)
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AgentEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "server_name", nullable = false, length = 100)
    private String serverName;

    @Setter(AccessLevel.NONE)
    @Column(name = "public_key", nullable = false, length = 64)
    private String publicKey;

    @Setter(AccessLevel.NONE)
    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AgentLifecycleStatus status;

    @Column(name = "enrollment_token_id", nullable = false)
    private UUID enrollmentTokenId;

    @Setter(AccessLevel.NONE)
    @CreatedDate
    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt;

    @Column(name = "enrolled_by", nullable = false)
    private UUID enrolledBy;

    @Column(name = "first_connected_at")
    private Instant firstConnectedAt;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    public AgentEntity(String serverName, String publicKey, String fingerprint,
                       UUID enrollmentTokenId, UUID enrolledBy) {
        this.serverName = serverName;
        this.publicKey = publicKey;
        this.fingerprint = fingerprint;
        this.enrollmentTokenId = enrollmentTokenId;
        this.enrolledBy = enrolledBy;
        this.status = AgentLifecycleStatus.ENROLLED;
    }
}
