package com.synapsys.api.infrastructure.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

@Component
class CaffeineAttemptTracker implements AttemptTracker {

    private final LongSupplier clock;

    // Single bounded cache — composite key encodes the window to avoid cross-window collisions.
    // TTL is a safe upper bound over the longest expected rate-limit window.
    private final Cache<String, Deque<Long>> cache = Caffeine.newBuilder()
        .maximumSize(100_000)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build();

    CaffeineAttemptTracker() {
        this(System::currentTimeMillis);
    }

    CaffeineAttemptTracker(LongSupplier clock) {
        this.clock = clock;
    }

    @Override
    public boolean isLimitExceeded(String key, int max, int windowSeconds) {
        long windowMs = windowSeconds * 1_000L;
        String cacheKey = windowSeconds + ":" + key;
        Deque<Long> timestamps = cache.get(cacheKey, k -> new ArrayDeque<>());
        long now = clock.getAsLong();
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < now - windowMs) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= max) return true;
            timestamps.addLast(now);
            return false;
        }
    }
}
