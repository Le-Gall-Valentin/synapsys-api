package com.synapsys.api.auth.infrastructure.persistence.adapter;

import com.synapsys.api.auth.domain.model.RefreshToken;
import com.synapsys.api.auth.domain.port.out.RefreshTokenMaintenancePort;
import com.synapsys.api.auth.domain.port.out.RefreshTokenRepository;
import com.synapsys.api.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.synapsys.api.auth.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository, RefreshTokenMaintenancePort {

    private final RefreshTokenJpaRepository jpa;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public boolean tryMarkUsedAndRevoke(UUID tokenId) {
        return jpa.markUsedAndRevokeIfNotRevokedById(tokenId) > 0;
    }

    @Override
    public void revoke(UUID tokenId) {
        jpa.revokeById(tokenId);
    }

    @Override
    public void revokeAllForUser(UUID userId) {
        jpa.revokeAllByUserId(userId);
    }

    @Override
    public int deleteExpiredAndRevoked(Instant now, Instant cutoff) {
        return jpa.deleteExpiredAndOldRevoked(now, cutoff);
    }

    @Override
    public RefreshToken save(UUID userId, String tokenHash, Instant expiresAt) {
        RefreshTokenEntity entity = new RefreshTokenEntity(userId, tokenHash, expiresAt);
        RefreshTokenEntity saved = jpa.save(entity);
        return toDomain(saved);
    }

    private RefreshToken toDomain(RefreshTokenEntity e) {
        return new RefreshToken(e.getId(), e.getUserId(), e.getTokenHash(),
            e.getExpiresAt(), e.isRevoked(), e.getCreatedAt(), e.getLastUsedAt());
    }
}