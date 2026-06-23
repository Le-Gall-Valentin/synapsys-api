package com.synapsys.api.agent.domain.port.out;

import com.synapsys.api.agent.domain.model.IssuedToken;

import java.time.Duration;
import java.util.UUID;

public interface EnrollmentTokenIssuerPort {
    /** Generates a high-entropy token, persists its hash, and returns the one-time clear value.
     *  {@code requestedTtl} null means use the configured maximum validity. */
    IssuedToken issue(String serverName, Duration requestedTtl, UUID createdBy);
}
