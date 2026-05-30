package com.synapsys.api.auth.domain.port.out;

import java.util.UUID;

public interface UserTotpPort {
    void saveTotpSecret(UUID userId, String secret);

    /**
     * Atomically saves the secret only if the user currently has no secret (DB-level WHERE secret IS NULL).
     * Returns true if saved, false if a secret was already present (concurrent write or repeated call).
     * Callers must reload the user and return the existing secret when false is returned.
     */
    boolean saveTotpSecretIfAbsent(UUID userId, String secret);

    void enableTotp(UUID userId);
    void disableTotp(UUID userId);
}