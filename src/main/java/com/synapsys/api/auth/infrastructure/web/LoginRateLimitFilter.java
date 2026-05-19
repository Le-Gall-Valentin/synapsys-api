package com.synapsys.api.auth.infrastructure.web;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);
    private static final String LOGIN_PATH = "/api/auth/login";
    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS = 60_000L;

    private final LongSupplier clock;

    // Bounded cache: max 10 000 IPs, entries expire after WINDOW_MS + 1s of inactivity
    private final Cache<String, Deque<Long>> attempts = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterAccess(WINDOW_MS + 1_000, TimeUnit.MILLISECONDS)
        .build();

    public LoginRateLimitFilter() {
        this(System::currentTimeMillis);
    }

    public LoginRateLimitFilter(LongSupplier clock) {
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(request.getMethod())
                && LOGIN_PATH.equals(request.getRequestURI())) {
            String ip = request.getRemoteAddr();
            if (isRateLimited(ip)) {
                log.warn("Rate limit exceeded for IP: {}", ip);
                response.setStatus(429);
                response.setContentType("application/problem+json");
                response.getWriter().write(
                    "{\"status\":429,\"title\":\"Too Many Requests\",\"detail\":\"Too many login attempts. Try again later.\"}"
                );
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isRateLimited(String ip) {
        long now = clock.getAsLong();
        Deque<Long> timestamps = attempts.get(ip, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < now - WINDOW_MS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_ATTEMPTS) {
                return true;
            }
            timestamps.addLast(now);
            return false;
        }
    }
}