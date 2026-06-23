package com.synapsys.api.agent.infrastructure.ws;

import com.synapsys.api.agent.infrastructure.config.AgentProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentConnectionLimiterTest {

    private AgentConnectionLimiter limiter(int maxPerIp) {
        return new AgentConnectionLimiter(new AgentProperties(24, 30, 90, maxPerIp, "synenr_", "/ws/agents"));
    }

    @Test
    void acquiresUpToTheCapThenRejects() {
        AgentConnectionLimiter limiter = limiter(2);

        assertThat(limiter.tryAcquire("1.2.3.4")).isTrue();
        assertThat(limiter.tryAcquire("1.2.3.4")).isTrue();
        assertThat(limiter.tryAcquire("1.2.3.4")).isFalse();
    }

    @Test
    void releaseFreesASlot() {
        AgentConnectionLimiter limiter = limiter(1);
        assertThat(limiter.tryAcquire("1.2.3.4")).isTrue();
        assertThat(limiter.tryAcquire("1.2.3.4")).isFalse();

        limiter.release("1.2.3.4");

        assertThat(limiter.tryAcquire("1.2.3.4")).isTrue();
    }

    @Test
    void capIsTrackedPerIp() {
        AgentConnectionLimiter limiter = limiter(1);
        assertThat(limiter.tryAcquire("1.1.1.1")).isTrue();
        assertThat(limiter.tryAcquire("2.2.2.2")).isTrue();
        assertThat(limiter.tryAcquire("1.1.1.1")).isFalse();
    }

    @Test
    void releaseOfUnknownIpIsHarmless() {
        AgentConnectionLimiter limiter = limiter(1);
        limiter.release("9.9.9.9"); // must not throw nor create negative budget
        assertThat(limiter.tryAcquire("9.9.9.9")).isTrue();
    }
}