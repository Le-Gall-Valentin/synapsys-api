package com.synapsys.api.infrastructure.ratelimit;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException() {
        super("Too many requests. Please try again later.");
    }
}