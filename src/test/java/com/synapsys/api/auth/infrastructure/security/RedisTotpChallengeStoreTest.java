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
    void isCodeAlreadyUsed_existingKey_returnsTrue() {
        when(valueOps.get("totp:used:" + userId + ":123456")).thenReturn("1");

        assertThat(store.isCodeAlreadyUsed(userId, "123456")).isTrue();
    }

    @Test
    void isCodeAlreadyUsed_missingKey_returnsFalse() {
        when(valueOps.get("totp:used:" + userId + ":123456")).thenReturn(null);

        assertThat(store.isCodeAlreadyUsed(userId, "123456")).isFalse();
    }

    @Test
    void markCodeUsed_storesKeyWithAntiReplayTtl() {
        store.markCodeUsed(userId, "654321");

        verify(valueOps).set(
            eq("totp:used:" + userId + ":654321"),
            eq("1"),
            eq(Duration.ofSeconds(90))
        );
    }
}