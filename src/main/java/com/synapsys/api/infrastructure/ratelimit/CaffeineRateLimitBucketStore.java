package com.synapsys.api.infrastructure.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.EstimationProbe;
import io.github.bucket4j.TimeMeter;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class CaffeineRateLimitBucketStore implements RateLimitBucketStore {

    private final TimeMeter clock;
    private final Cache<String, Bucket> buckets;

    CaffeineRateLimitBucketStore() {
        this(TimeMeter.SYSTEM_NANOTIME);
    }

    CaffeineRateLimitBucketStore(TimeMeter clock) {
        this.clock = clock;
        this.buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();
    }

    @Override
    public BucketResult tryConsume(String key, int max, int windowSeconds) {
        Bucket bucket = getOrCreate(key, max, windowSeconds);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        return new BucketResult(probe.isConsumed(), probe.getRemainingTokens(), probe.getNanosToWaitForRefill());
    }

    @Override
    public BucketResult peekConsume(String key, int max, int windowSeconds) {
        Bucket bucket = getOrCreate(key, max, windowSeconds);
        EstimationProbe probe = bucket.estimateAbilityToConsume(1);
        return new BucketResult(probe.canBeConsumed(), probe.getRemainingTokens(), probe.getNanosToWaitForRefill());
    }

    private Bucket getOrCreate(String key, int max, int windowSeconds) {
        return buckets.get(key, k -> Bucket.builder()
            .withCustomTimePrecision(clock)
            .addLimit(Bandwidth.builder()
                .capacity(max)
                .refillGreedy(max, Duration.ofSeconds(windowSeconds))
                .build())
            .build());
    }

    public void clearAll() {
        buckets.invalidateAll();
    }
}