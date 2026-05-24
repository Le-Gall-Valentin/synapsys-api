package com.synapsys.api.auth.infrastructure.persistence.repository;

import com.synapsys.api.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshTokenEntity t SET t.revoked = true, t.lastUsedAt = CURRENT_TIMESTAMP WHERE t.id = :id AND t.revoked = false")
    int markUsedAndRevokeIfNotRevokedById(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshTokenEntity t SET t.revoked = true WHERE t.id = :id")
    void revokeById(UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshTokenEntity t SET t.revoked = true WHERE t.userId = :userId")
    void revokeAllByUserId(UUID userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshTokenEntity t WHERE t.expiresAt < :now OR (t.revoked = true AND (t.lastUsedAt < :cutoff OR (t.lastUsedAt IS NULL AND t.createdAt < :cutoff)))")
    int deleteExpiredAndOldRevoked(@Param("now") Instant now, @Param("cutoff") Instant cutoff);
}