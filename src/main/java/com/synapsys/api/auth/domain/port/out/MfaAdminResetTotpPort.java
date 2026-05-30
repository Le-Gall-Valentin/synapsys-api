package com.synapsys.api.auth.domain.port.out;

import java.util.UUID;

public interface MfaAdminResetTotpPort {
    void disableTotpIfEnabled(UUID userId);
}
