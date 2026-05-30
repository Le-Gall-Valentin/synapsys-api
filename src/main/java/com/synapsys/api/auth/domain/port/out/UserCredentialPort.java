package com.synapsys.api.auth.domain.port.out;

import java.util.UUID;

public interface UserCredentialPort {
    void saveCredential(UUID userId, String passwordHash);
}