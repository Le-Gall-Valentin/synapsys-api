package com.synapsys.api.auth.infrastructure.persistence.adapter;

import com.synapsys.api.auth.domain.model.RefreshToken;
import com.synapsys.api.auth.domain.port.out.RefreshTokenRepository;
import com.synapsys.api.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.synapsys.api.auth.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(RefreshToken token) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(token.userId());
        entity.setTokenHash(token.tokenHash());
        entity.setExpiresAt(token.expiresAt());
        entity.setRevoked(token.revoked());
        entity.setLastUsedAt(token.lastUsedAt());
        jpa.save(entity);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String sha256Hash) {
        return jpa.findByTokenHash(sha256Hash).map(this::toDomain);
    }

    @Override
    @Transactional
    public void revoke(UUID tokenId) {
        jpa.revokeById(tokenId);
    }

    @Override
    @Transactional
    public void revokeAllForUser(UUID userId) {
        jpa.revokeAllByUserId(userId);
    }

    private RefreshToken toDomain(RefreshTokenEntity e) {
        return new RefreshToken(e.getId(), e.getUserId(), e.getTokenHash(),
            e.getExpiresAt(), e.isRevoked(), e.getCreatedAt(), e.getLastUsedAt());
    }
}