package com.synapsys.api.agent.infrastructure.redis;

import com.synapsys.api.agent.infrastructure.config.AgentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisAgentPresenceStoreTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;

    private RedisAgentPresenceStore store;
    private final UUID agentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        store = new RedisAgentPresenceStore(redis, new AgentProperties(24, 30, 90, "synenr_", "/ws/agents"));
    }

    @Test
    void markPresent_setsKeyWithTtl() {
        Instant at = Instant.ofEpochMilli(1000);
        store.markPresent(agentId, "node-A", "1.2.3.4", at);
        verify(valueOps).set(eq("agent:presence:" + agentId), eq("node-A|1.2.3.4|1000"), eq(Duration.ofSeconds(90)));
    }

    @Test
    void refresh_extendsTtl() {
        store.refresh(agentId);
        verify(redis).expire("agent:presence:" + agentId, Duration.ofSeconds(90));
    }

    @Test
    void clear_deletesKey() {
        store.clear(agentId);
        verify(redis).delete("agent:presence:" + agentId);
    }

    @Test
    void presentAgentIds_returnsOnlyKeysWithValues() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        // emulate: only first key present
        when(valueOps.multiGet(List.of("agent:presence:" + a, "agent:presence:" + b)))
            .thenReturn(java.util.Arrays.asList("node|ip|1", null));
        Set<UUID> present = store.presentAgentIds(List.of(a, b));
        assertThat(present).containsExactlyInAnyOrder(a);
    }
}
