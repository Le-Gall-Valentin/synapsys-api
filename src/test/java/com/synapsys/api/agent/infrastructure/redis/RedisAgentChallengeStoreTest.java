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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisAgentChallengeStoreTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;

    private RedisAgentChallengeStore store;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        store = new RedisAgentChallengeStore(redis, new AgentProperties(24, 30, 90, "synenr_", "/ws/agents"));
    }

    @Test
    void issueChallenge_storesBase64NonceWithTtl_andReturnsIt() {
        String nonce = store.issueChallenge("conn-1");
        assertThat(nonce).isNotBlank();
        verify(valueOps).set(eq("agent:challenge:conn-1"), eq(nonce), eq(Duration.ofSeconds(30)));
    }

    @Test
    void consumeChallenge_returnsAndDeletesAtomically() {
        when(valueOps.getAndDelete("agent:challenge:conn-1")).thenReturn("nonce-value");
        Optional<String> result = store.consumeChallenge("conn-1");
        assertThat(result).contains("nonce-value");
    }

    @Test
    void consumeChallenge_missing_returnsEmpty() {
        when(valueOps.getAndDelete("agent:challenge:conn-2")).thenReturn(null);
        assertThat(store.consumeChallenge("conn-2")).isEmpty();
    }
}
