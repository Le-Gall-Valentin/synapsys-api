package com.synapsys.api.agent.infrastructure.persistence.repository;

import com.synapsys.api.agent.infrastructure.persistence.entity.EnrollmentTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentTokenJpaRepository extends JpaRepository<EnrollmentTokenEntity, UUID> {

    Optional<EnrollmentTokenEntity> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("""
        UPDATE EnrollmentTokenEntity t SET t.consumedAt = :now
        WHERE t.id = :id AND t.consumedAt IS NULL AND t.revokedAt IS NULL AND t.expiresAt > :now
        """)
    int markConsumed(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("""
        UPDATE EnrollmentTokenEntity t SET t.revokedAt = :now, t.revokedBy = :revokedBy
        WHERE t.id = :id AND t.consumedAt IS NULL AND t.revokedAt IS NULL AND t.expiresAt > :now
        """)
    int markRevoked(@Param("id") UUID id, @Param("revokedBy") UUID revokedBy, @Param("now") Instant now);
}
