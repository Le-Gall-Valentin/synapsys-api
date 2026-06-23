package com.synapsys.api.agent.infrastructure.persistence.entity;

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
    name = "agent_enrollment_tokens",
    uniqueConstraints = @UniqueConstraint(name = "uq_agent_enrollment_tokens_token_hash", columnNames = "token_hash"),
    indexes = @Index(name = "idx_agent_enrollment_tokens_created_by", columnList = "created_by")
)
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EnrollmentTokenEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "server_name", nullable = false, length = 100)
    private String serverName;

    @Setter(AccessLevel.NONE)
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Setter(AccessLevel.NONE)
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    public EnrollmentTokenEntity(String serverName, String tokenHash, Instant expiresAt, UUID createdBy) {
        this.serverName = serverName;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdBy = createdBy;
    }
}
