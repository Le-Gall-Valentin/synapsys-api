package com.synapsys.api.auth.domain.port.out;

import java.util.Optional;
import java.util.UUID;

public interface TotpChallengeStorePort {
    String createChallenge(UUID userId);
    Optional<UUID> resolveChallenge(String challengeId);
    void invalidateChallenge(String challengeId);
    boolean isCodeAlreadyUsed(UUID userId, String code);
    void markCodeUsed(UUID userId, String code);
}