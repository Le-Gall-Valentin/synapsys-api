package com.synapsys.api.mfa.application.port.in;

import java.util.UUID;

public interface VerifyTotpCodeUseCase {
    boolean verifyAndConsume(UUID userId, String code);
}