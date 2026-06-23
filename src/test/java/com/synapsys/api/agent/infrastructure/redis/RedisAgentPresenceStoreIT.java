package com.synapsys.api.agent.infrastructure.redis;

import com.synapsys.api.IntegrationTestConfig;
import com.synapsys.api.agent.domain.port.out.AgentPresencePort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class RedisAgentPresenceStoreIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container @ServiceConnection @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired AgentPresencePort presence;

    @Test
    void markPresent_thenIsPresent_andPresentAgentIds() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        presence.markPresent(a, "node-A", "1.1.1.1", Instant.now());

        assertThat(presence.isPresent(a)).isTrue();
        assertThat(presence.isPresent(b)).isFalse();
        assertThat(presence.presentAgentIds(List.of(a, b))).containsExactly(a);
    }

    @Test
    void clearIfOwnedBy_removesOnlyWhenNodeMatches() {
        UUID agentId = UUID.randomUUID();
        presence.markPresent(agentId, "node-A", "1.1.1.1", Instant.now());

        // Another node must not be able to clear presence it does not own.
        assertThat(presence.clearIfOwnedBy(agentId, "node-B")).isFalse();
        assertThat(presence.isPresent(agentId)).isTrue();

        // The owning node clears it.
        assertThat(presence.clearIfOwnedBy(agentId, "node-A")).isTrue();
        assertThat(presence.isPresent(agentId)).isFalse();
    }

    @Test
    void clearIfOwnedBy_afterOwnershipMovedToAnotherNode_isNoOp() {
        UUID agentId = UUID.randomUUID();
        // Agent connected on node-A, then reconnected to node-B (node-B re-asserted ownership).
        presence.markPresent(agentId, "node-A", "1.1.1.1", Instant.now());
        presence.markPresent(agentId, "node-B", "2.2.2.2", Instant.now());

        // The stale node-A disconnect must not clear the live presence owned by node-B.
        assertThat(presence.clearIfOwnedBy(agentId, "node-A")).isFalse();
        assertThat(presence.isPresent(agentId)).isTrue();
    }

    @Test
    void clearIfOwnedBy_missingKey_isNoOp() {
        assertThat(presence.clearIfOwnedBy(UUID.randomUUID(), "node-A")).isFalse();
    }
}
