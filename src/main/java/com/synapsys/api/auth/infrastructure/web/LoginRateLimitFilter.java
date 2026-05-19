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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);
    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REFRESH_PATH = "/api/auth/refresh";
    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS = 60_000L;

    private final LongSupplier clock;
    private final List<String> trustedProxies;

    // Bounded cache: max 10 000 IPs, entries expire after WINDOW_MS + 1s of inactivity
    private final Cache<String, Deque<Long>> attempts = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterAccess(WINDOW_MS + 1_000, TimeUnit.MILLISECONDS)
        .build();

    public LoginRateLimitFilter(LongSupplier clock, List<String> trustedProxies) {
        this.clock = clock;
        this.trustedProxies = List.copyOf(trustedProxies);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if ("POST".equalsIgnoreCase(request.getMethod())
                && (LOGIN_PATH.equals(uri) || REFRESH_PATH.equals(uri))) {
            String ip = resolveClientIp(request);
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

    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxies.contains(remoteAddr)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
        }
        return remoteAddr;
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