package com.synapsys.api.mfa.application.port.in;

import java.util.UUID;

public interface GetTotpStatusUseCase {
    boolean isTotpEnabled(UUID userId);
}