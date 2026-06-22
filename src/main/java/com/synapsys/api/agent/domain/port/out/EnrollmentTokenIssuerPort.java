package com.synapsys.api.agent.domain.port.out;

import com.synapsys.api.agent.domain.model.IssuedToken;

import java.util.UUID;

public interface EnrollmentTokenIssuerPort {
    /** Generates a high-entropy token, persists its hash, and returns the one-time clear value. */
    IssuedToken issue(String serverName, UUID createdBy);
}
