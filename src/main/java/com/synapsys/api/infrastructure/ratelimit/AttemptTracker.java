package com.synapsys.api.infrastructure.ratelimit;

interface AttemptTracker {

    /**
     * Records an attempt for {@code key} and returns {@code true} if the limit is exceeded.
     *
     * @param key           opaque identifier (IP address, "user:alice", …)
     * @param max           maximum allowed attempts in the window
     * @param windowSeconds rolling window duration in seconds
     */
    boolean isLimitExceeded(String key, int max, int windowSeconds);
}