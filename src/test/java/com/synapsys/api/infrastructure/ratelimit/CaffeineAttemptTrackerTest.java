package com.synapsys.api.infrastructure.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class CaffeineAttemptTrackerTest {

    private final AtomicLong clock = new AtomicLong(0);
    private CaffeineAttemptTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new CaffeineAttemptTracker(clock::get);
    }

    @Test
    void maxAttemptsAreAllowed() {
        for (int i = 0; i < 10; i++) {
            assertThat(tracker.isLimitExceeded("ip:1.2.3.4", 10, 60))
                .as("attempt %d", i + 1).isFalse();
        }
    }

    @Test
    void exceedingMaxIsRateLimited() {
        for (int i = 0; i < 10; i++) {
            tracker.isLimitExceeded("ip:5.5.5.5", 10, 60);
        }
        assertThat(tracker.isLimitExceeded("ip:5.5.5.5", 10, 60)).isTrue();
    }

    @Test
    void attemptsOutsideWindowAreEvicted() {
        for (int i = 0; i < 10; i++) {
            tracker.isLimitExceeded("ip:7.7.7.7", 10, 60);
        }
        clock.set(61_000L);
        assertThat(tracker.isLimitExceeded("ip:7.7.7.7", 10, 60)).isFalse();
    }

    @Test
    void differentKeysDontInterfere() {
        for (int i = 0; i < 10; i++) {
            tracker.isLimitExceeded("ip:10.0.0.1", 10, 60);
        }
        assertThat(tracker.isLimitExceeded("ip:10.0.0.2", 10, 60)).isFalse();
    }

    @Test
    void differentWindowSizesDontInterfere() {
        // 10 attempts with 60s window
        for (int i = 0; i < 10; i++) {
            tracker.isLimitExceeded("user:alice", 10, 60);
        }
        // Same key but different window (300s) should NOT be affected
        assertThat(tracker.isLimitExceeded("user:alice", 10, 300)).isFalse();
    }

    @Test
    void limitIsEnforcedPerKey() {
        for (int i = 0; i < 20; i++) {
            tracker.isLimitExceeded("user:target", 20, 300);
        }
        assertThat(tracker.isLimitExceeded("user:target", 20, 300)).isTrue();
        assertThat(tracker.isLimitExceeded("user:other", 20, 300)).isFalse();
    }
}