package com.synapsys.api.auth.infrastructure.persistence.repository;

import com.synapsys.api.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshTokenEntity t SET t.revoked = true WHERE t.id = :id")
    void revokeById(UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshTokenEntity t SET t.revoked = true WHERE t.userId = :userId")
    void revokeAllByUserId(UUID userId);
}