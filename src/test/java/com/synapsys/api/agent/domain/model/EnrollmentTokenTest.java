package com.synapsys.api.agent.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnrollmentTokenTest {

    private final Instant now = Instant.parse("2026-06-22T12:00:00Z");
    private final UUID creator = UUID.randomUUID();

    private EnrollmentToken token(Instant consumedAt, Instant revokedAt, Instant expiresAt) {
        return new EnrollmentToken(UUID.randomUUID(), "web-01", consumedAt, revokedAt,
            revokedAt == null ? null : creator, expiresAt, now.minus(1, ChronoUnit.HOURS), creator);
    }

    @Test
    void deriveStatus_active_whenNotConsumedRevokedOrExpired() {
        assertThat(token(null, null, now.plus(1, ChronoUnit.HOURS)).deriveStatus(now))
            .isEqualTo(EnrollmentTokenStatus.ACTIVE);
    }

    @Test
    void deriveStatus_consumed_takesPrecedenceOverExpiry() {
        assertThat(token(now.minus(2, ChronoUnit.HOURS), null, now.minus(1, ChronoUnit.HOURS)).deriveStatus(now))
            .isEqualTo(EnrollmentTokenStatus.CONSUMED);
    }

    @Test
    void deriveStatus_revoked_takesPrecedenceOverConsumed() {
        assertThat(token(now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS)).deriveStatus(now))
            .isEqualTo(EnrollmentTokenStatus.REVOKED);
    }

    @Test
    void deriveStatus_expired_atOrAfterExpiry() {
        assertThat(token(null, null, now).deriveStatus(now)).isEqualTo(EnrollmentTokenStatus.EXPIRED);
    }

    @Test
    void ensureRevocable_passesWhenActive_throwsOtherwise() {
        assertThatCode(() -> token(null, null, now.plus(1, ChronoUnit.HOURS)).ensureRevocable(now))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> token(now, null, now.plus(1, ChronoUnit.HOURS)).ensureRevocable(now))
            .isInstanceOf(AgentException.TokenNotRevocable.class);
    }
}
