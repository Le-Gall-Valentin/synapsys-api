package com.synapsys.api.auth.infrastructure.security;

import com.synapsys.api.infrastructure.config.SynapsysProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class CookieService {

    public static final String ACCESS_COOKIE = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";

    private final boolean secure;
    private final long accessMaxAgeSeconds;
    private final long refreshMaxAgeSeconds;

    public CookieService(SynapsysProperties properties) {
        this.secure = properties.cookie().secure();
        this.accessMaxAgeSeconds = properties.jwt().expiryMinutes() * 60L;
        this.refreshMaxAgeSeconds = (long) properties.refreshToken().expiryDays() * 86_400L;
    }

    public ResponseCookie buildAccessCookie(String token) {
        return build(ACCESS_COOKIE, token, "/api", accessMaxAgeSeconds);
    }

    public ResponseCookie buildRefreshCookie(String token) {
        return build(REFRESH_COOKIE, token, "/api/auth", refreshMaxAgeSeconds);
    }

    public List<ResponseCookie> buildClearCookies() {
        return List.of(
            build(ACCESS_COOKIE, "", "/api", 0),
            build(REFRESH_COOKIE, "", "/api/auth", 0)
        );
    }

    public Optional<String> extractFromRequest(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
            .filter(c -> name.equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst();
    }

    private ResponseCookie build(String name, String value, String path, long maxAge) {
        return ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(secure)
            .path(path)
            .maxAge(Duration.ofSeconds(maxAge))
            .sameSite("Strict")
            .build();
    }
}