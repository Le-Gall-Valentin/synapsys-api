package com.synapsys.api.agent.domain.model;

import java.time.Duration;

/** Domain rule for resolving an enrollment token's effective time-to-live. */
public final class EnrollmentTokenTtl {

    private EnrollmentTokenTtl() {}

    /**
     * Resolves the effective TTL against the configured maximum.
     * A null {@code requested} means "use the maximum". The result must be strictly
     * positive and must not exceed {@code max}, otherwise {@link AgentException.InvalidTokenTtl}.
     */
    public static Duration resolve(Duration requested, Duration max) {
        Duration effective = requested == null ? max : requested;
        if (effective.isZero() || effective.isNegative() || effective.compareTo(max) > 0) {
            throw new AgentException.InvalidTokenTtl();
        }
        return effective;
    }
}