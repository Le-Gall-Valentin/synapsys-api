package com.synapsys.api.auth.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);
    private static final String LOGIN_PATH = "/api/auth/login";
    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS = 60_000L;

    private final ConcurrentHashMap<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(request.getMethod())
                && LOGIN_PATH.equals(request.getRequestURI())) {
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

    private boolean isRateLimited(String ip) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = attempts.computeIfAbsent(ip, k -> new ArrayDeque<>());
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

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}