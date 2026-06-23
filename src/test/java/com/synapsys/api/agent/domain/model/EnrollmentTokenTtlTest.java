package com.synapsys.api.agent.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnrollmentTokenTtlTest {

    private static final Duration MAX = Duration.ofHours(24);

    @Test
    void resolve_nullRequested_usesMaximum() {
        assertThat(EnrollmentTokenTtl.resolve(null, MAX)).isEqualTo(MAX);
    }

    @Test
    void resolve_shorterRequested_isHonored() {
        assertThat(EnrollmentTokenTtl.resolve(Duration.ofMinutes(15), MAX)).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void resolve_exactlyMaximum_isAllowed() {
        assertThat(EnrollmentTokenTtl.resolve(MAX, MAX)).isEqualTo(MAX);
    }

    @Test
    void resolve_aboveMaximum_throws() {
        assertThatThrownBy(() -> EnrollmentTokenTtl.resolve(Duration.ofHours(25), MAX))
            .isInstanceOf(AgentException.InvalidTokenTtl.class);
    }

    @Test
    void resolve_zeroOrNegative_throws() {
        assertThatThrownBy(() -> EnrollmentTokenTtl.resolve(Duration.ZERO, MAX))
            .isInstanceOf(AgentException.InvalidTokenTtl.class);
        assertThatThrownBy(() -> EnrollmentTokenTtl.resolve(Duration.ofMinutes(-5), MAX))
            .isInstanceOf(AgentException.InvalidTokenTtl.class);
    }
}