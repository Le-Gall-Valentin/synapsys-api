package com.synapsys.api.auth.domain.port.out;

import com.synapsys.api.auth.domain.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken token);
    Optional<RefreshToken> findByTokenHash(String sha256Hash);
    void markUsed(UUID tokenId);
    void revoke(UUID tokenId);
    void revokeAllForUser(UUID userId);
}
