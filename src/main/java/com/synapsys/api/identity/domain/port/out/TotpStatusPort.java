package com.synapsys.api.identity.domain.port.out;

import java.util.UUID;

public interface TotpStatusPort {
    boolean isTotpEnabled(UUID userId);
}