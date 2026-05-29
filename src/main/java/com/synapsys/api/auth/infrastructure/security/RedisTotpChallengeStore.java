package com.synapsys.api.auth.infrastructure.security;

import com.synapsys.api.auth.domain.port.out.TotpChallengeStorePort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class RedisTotpChallengeStore implements TotpChallengeStorePort {

    private static final String CHALLENGE_PREFIX = "totp:challenge:";
    private static final String USED_CODE_PREFIX = "totp:used:";
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(15);
    private static final Duration ANTI_REPLAY_TTL = Duration.ofSeconds(90);

    private final StringRedisTemplate redisTemplate;

    public RedisTotpChallengeStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String createChallenge(UUID userId) {
        String challengeId = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(CHALLENGE_PREFIX + challengeId, userId.toString(), CHALLENGE_TTL);
        return challengeId;
    }

    @Override
    public Optional<UUID> resolveChallenge(String challengeId) {
        String value = redisTemplate.opsForValue().get(CHALLENGE_PREFIX + challengeId);
        if (value == null) return Optional.empty();
        return Optional.of(UUID.fromString(value));
    }

    @Override
    public void invalidateChallenge(String challengeId) {
        redisTemplate.delete(CHALLENGE_PREFIX + challengeId);
    }

    @Override
    public boolean isCodeAlreadyUsed(UUID userId, String code) {
        return redisTemplate.opsForValue().get(USED_CODE_PREFIX + userId + ":" + code) != null;
    }

    @Override
    public void markCodeUsed(UUID userId, String code) {
        redisTemplate.opsForValue().set(USED_CODE_PREFIX + userId + ":" + code, "1", ANTI_REPLAY_TTL);
    }
}