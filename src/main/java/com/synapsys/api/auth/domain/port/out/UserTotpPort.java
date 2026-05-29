package com.synapsys.api.auth.domain.port.out;

import java.util.UUID;

public interface UserTotpPort {
    void saveTotpSecret(UUID userId, String secret);
    void enableTotp(UUID userId);
    void disableTotp(UUID userId);
}