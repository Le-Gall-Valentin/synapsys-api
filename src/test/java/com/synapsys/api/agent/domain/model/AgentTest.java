package com.synapsys.api.agent.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentTest {

    private Agent agent(AgentLifecycleStatus status, Instant firstConnectedAt) {
        return new Agent(UUID.randomUUID(), "web-01", new byte[32], "fp", status,
            UUID.randomUUID(), Instant.now(), UUID.randomUUID(), firstConnectedAt,
            null, null, status == AgentLifecycleStatus.REVOKED ? Instant.now() : null, null);
    }

    @Test
    void deriveStatus_revoked_whenLifecycleRevoked_regardlessOfPresence() {
        assertThat(agent(AgentLifecycleStatus.REVOKED, Instant.now()).deriveStatus(true))
            .isEqualTo(DerivedAgentStatus.REVOKED);
    }

    @Test
    void deriveStatus_active_whenPresent() {
        assertThat(agent(AgentLifecycleStatus.ENROLLED, Instant.now()).deriveStatus(true))
            .isEqualTo(DerivedAgentStatus.ACTIVE);
    }

    @Test
    void deriveStatus_pending_whenNeverConnectedAndDown() {
        assertThat(agent(AgentLifecycleStatus.ENROLLED, null).deriveStatus(false))
            .isEqualTo(DerivedAgentStatus.PENDING);
    }

    @Test
    void deriveStatus_inactive_whenPreviouslyConnectedAndDown() {
        assertThat(agent(AgentLifecycleStatus.ENROLLED, Instant.now()).deriveStatus(false))
            .isEqualTo(DerivedAgentStatus.INACTIVE);
    }

    @Test
    void ensureRevocable_throwsWhenAlreadyRevoked() {
        assertThatCode(() -> agent(AgentLifecycleStatus.ENROLLED, null).ensureRevocable()).doesNotThrowAnyException();
        assertThatThrownBy(() -> agent(AgentLifecycleStatus.REVOKED, null).ensureRevocable())
            .isInstanceOf(AgentException.AgentNotRevocable.class);
    }

    @Test
    void ensureDeletable_requiresRevoked() {
        assertThatCode(() -> agent(AgentLifecycleStatus.REVOKED, null).ensureDeletable()).doesNotThrowAnyException();
        assertThatThrownBy(() -> agent(AgentLifecycleStatus.ENROLLED, null).ensureDeletable())
            .isInstanceOf(AgentException.AgentNotDeletable.class);
    }

    @Test
    void ensureConnectable_throwsWhenRevoked() {
        assertThatCode(() -> agent(AgentLifecycleStatus.ENROLLED, null).ensureConnectable()).doesNotThrowAnyException();
        assertThatThrownBy(() -> agent(AgentLifecycleStatus.REVOKED, null).ensureConnectable())
            .isInstanceOf(AgentException.HandshakeFailed.class);
    }
}
