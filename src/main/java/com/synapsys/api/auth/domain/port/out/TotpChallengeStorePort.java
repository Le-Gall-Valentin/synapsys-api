package com.synapsys.api.auth.domain.port.out;

import java.util.Optional;
import java.util.UUID;

public interface TotpChallengeStorePort {
    String createChallenge(UUID userId);
    Optional<UUID> resolveChallenge(String challengeId);
    void invalidateChallenge(String challengeId);

    /**
     * Atomically marks a TOTP code as used for the given user (Redis SETNX).
     * Returns true if the code was freshly consumed, false if it was already used (replay).
     * TTL covers the full cryptographic validity window to prevent late replays.
     */
    boolean markCodeUsedIfAbsent(UUID userId, String code);

    /**
     * Increments the failed-attempt counter for a challenge and returns the new count.
     * TTL is set only on the first increment (matches challenge TTL).
     * Callers must invalidate the challenge when the count reaches the configured maximum.
     */
    int incrementFailedAttempts(String challengeId);
}