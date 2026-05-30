package com.synapsys.api.mfa.domain.port.out;

import java.util.UUID;

public interface UserTotpPort {
    void createDefaultRecord(UUID userId);
    void saveTotpSecret(UUID userId, String secret);
    boolean saveTotpSecretIfAbsent(UUID userId, String secret);
    void enableTotp(UUID userId);
    void disableTotp(UUID userId);
}