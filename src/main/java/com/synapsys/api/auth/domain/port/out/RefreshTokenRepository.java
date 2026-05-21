package com.synapsys.api.auth.domain.port.out;

import com.synapsys.api.auth.domain.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    boolean tryMarkUsedAndRevoke(UUID tokenId);
    void revoke(UUID tokenId);
    void revokeAllForUser(UUID userId);
    int deleteExpiredAndRevoked(java.time.Instant now, java.time.Instant cutoff);
}
