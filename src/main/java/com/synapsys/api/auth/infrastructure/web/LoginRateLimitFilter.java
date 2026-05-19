package com.synapsys.api.auth.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);
    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REFRESH_PATH = "/api/auth/refresh";

    private static final int IP_MAX_ATTEMPTS = 10;
    private static final long IP_WINDOW_MS = 60_000L;
    private static final int USERNAME_MAX_ATTEMPTS = 20;
    private static final long USERNAME_WINDOW_MS = 300_000L; // 5 minutes

    private final LongSupplier clock;
    private final List<String> trustedProxies;
    private final ObjectMapper objectMapper;

    // Bounded caches — entries expire after inactivity to cap memory usage
    private final Cache<String, Deque<Long>> ipAttempts = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterAccess(IP_WINDOW_MS + 1_000, TimeUnit.MILLISECONDS)
        .build();

    private final Cache<String, Deque<Long>> usernameAttempts = Caffeine.newBuilder()
        .maximumSize(50_000)
        .expireAfterAccess(USERNAME_WINDOW_MS + 1_000, TimeUnit.MILLISECONDS)
        .build();

    public LoginRateLimitFilter(LongSupplier clock, List<String> trustedProxies, ObjectMapper objectMapper) {
        this.clock = clock;
        this.trustedProxies = List.copyOf(trustedProxies);
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        boolean isPost = "POST".equalsIgnoreCase(request.getMethod());

        if (isPost && LOGIN_PATH.equals(uri)) {
            // Cache body so Spring can still read it for deserialization
            CachedBodyRequestWrapper wrapped = new CachedBodyRequestWrapper(request);
            String ip = resolveClientIp(wrapped);

            if (isRateLimited(ipAttempts, IP_WINDOW_MS, IP_MAX_ATTEMPTS, ip)) {
                log.warn("IP rate limit exceeded for IP: {}", ip);
                writeTooManyRequests(response);
                return;
            }

            String username = extractUsername(wrapped.cachedBody);
            if (username != null && isRateLimited(usernameAttempts, USERNAME_WINDOW_MS, USERNAME_MAX_ATTEMPTS,
                    username.toLowerCase(Locale.ROOT))) {
                log.warn("Username rate limit exceeded for user: {}", username);
                writeTooManyRequests(response);
                return;
            }

            chain.doFilter(wrapped, response);

        } else if (isPost && REFRESH_PATH.equals(uri)) {
            String ip = resolveClientIp(request);
            if (isRateLimited(ipAttempts, IP_WINDOW_MS, IP_MAX_ATTEMPTS, ip)) {
                log.warn("IP rate limit exceeded for IP: {}", ip);
                writeTooManyRequests(response);
                return;
            }
            chain.doFilter(request, response);

        } else {
            chain.doFilter(request, response);
        }
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

    private String extractUsername(byte[] body) {
        if (body.length == 0) return null;
        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode usernameNode = node.get("username");
            return usernameNode != null && usernameNode.isTextual() ? usernameNode.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isRateLimited(Cache<String, Deque<Long>> cache, long windowMs, int maxAttempts, String key) {
        long now = clock.getAsLong();
        Deque<Long> timestamps = cache.get(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < now - windowMs) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxAttempts) {
                return true;
            }
            timestamps.addLast(now);
            return false;
        }
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/problem+json");
        response.getWriter().write(
            "{\"status\":429,\"title\":\"Too Many Requests\",\"detail\":\"Too many login attempts. Try again later.\"}"
        );
    }

    private static final class CachedBodyRequestWrapper extends HttpServletRequestWrapper {
        final byte[] cachedBody;

        CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = request.getInputStream().readAllBytes();
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream delegate = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override public boolean isFinished() { return delegate.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) {}
                @Override public int read() { return delegate.read(); }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}