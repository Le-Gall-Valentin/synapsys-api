package com.synapsys.api.auth.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTotpChallengeStoreTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    private RedisTotpChallengeStore store;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        store = new RedisTotpChallengeStore(redisTemplate);
    }

    @Test
    void createChallenge_storesUserIdWithTtl_andReturnsUuid() {
        String challengeId = store.createChallenge(userId);

        assertThat(challengeId).isNotBlank();
        assertThat(UUID.fromString(challengeId)).isNotNull();
        verify(valueOps).set(
            eq("totp:challenge:" + challengeId),
            eq(userId.toString()),
            eq(Duration.ofMinutes(15))
        );
    }

    @Test
    void resolveChallenge_existingKey_returnsUserId() {
        String challengeId = UUID.randomUUID().toString();
        when(valueOps.get("totp:challenge:" + challengeId)).thenReturn(userId.toString());

        Optional<UUID> result = store.resolveChallenge(challengeId);

        assertThat(result).contains(userId);
    }

    @Test
    void resolveChallenge_missingKey_returnsEmpty() {
        String challengeId = UUID.randomUUID().toString();
        when(valueOps.get("totp:challenge:" + challengeId)).thenReturn(null);

        Optional<UUID> result = store.resolveChallenge(challengeId);

        assertThat(result).isEmpty();
    }

    @Test
    void invalidateChallenge_deletesKey() {
        String challengeId = UUID.randomUUID().toString();

        store.invalidateChallenge(challengeId);

        verify(redisTemplate).delete("totp:challenge:" + challengeId);
    }

    @Test
    void incrementFailedAttempts_firstAttempt_returns1_andSetsTtl() {
        String challengeId = UUID.randomUUID().toString();
        when(valueOps.increment("totp:attempts:" + challengeId)).thenReturn(1L);

        int count = store.incrementFailedAttempts(challengeId);

        assertThat(count).isEqualTo(1);
        verify(redisTemplate).expire(
            eq("totp:attempts:" + challengeId),
            eq(Duration.ofMinutes(15))
        );
    }

    @Test
    void incrementFailedAttempts_subsequentAttempts_doNotResetTtl() {
        String challengeId = UUID.randomUUID().toString();
        when(valueOps.increment("totp:attempts:" + challengeId)).thenReturn(3L);

        int count = store.incrementFailedAttempts(challengeId);

        assertThat(count).isEqualTo(3);
        verify(redisTemplate, never()).expire(any(), any(Duration.class));
    }
}