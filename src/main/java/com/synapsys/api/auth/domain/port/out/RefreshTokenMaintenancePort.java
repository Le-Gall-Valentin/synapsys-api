package com.synapsys.api.auth.domain.port.out;

import java.time.Instant;

public interface RefreshTokenMaintenancePort {
    /** Deletes tokens that are both expired and revoked. Returns count deleted. */
    int deleteExpiredAndRevoked(Instant now, Instant cutoff);
}