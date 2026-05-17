package com.synapsys.api.auth.infrastructure.persistence.adapter;

import com.synapsys.api.auth.domain.model.RefreshToken;
import com.synapsys.api.auth.domain.port.out.RefreshTokenRepository;
import com.synapsys.api.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.synapsys.api.auth.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(token.userId());
        entity.setTokenHash(token.tokenHash());
        entity.setExpiresAt(token.expiresAt());
        entity.setRevoked(token.revoked());
        entity.setLastUsedAt(token.lastUsedAt());
        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public void markUsed(UUID tokenId) {
        jpa.markUsedById(tokenId);
    }

    @Override
    public void revoke(UUID tokenId) {
        jpa.revokeById(tokenId);
    }

    @Override
    public void revokeAllForUser(UUID userId) {
        jpa.revokeAllByUserId(userId);
    }

    private RefreshToken toDomain(RefreshTokenEntity e) {
        return new RefreshToken(e.getId(), e.getUserId(), e.getTokenHash(),
            e.getExpiresAt(), e.isRevoked(), e.getCreatedAt(), e.getLastUsedAt());
    }
}