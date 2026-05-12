package com.synapsys.api.auth.infrastructure.security;

import com.synapsys.api.infrastructure.config.SynapsysProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class CookieService {

    public static final String ACCESS_COOKIE = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";

    private final boolean secure;

    public CookieService(SynapsysProperties properties) {
        this.secure = properties.cookie().secure();
    }

    public Cookie buildAccessCookie(String token, int maxAgeSeconds) {
        return build(ACCESS_COOKIE, token, "/api", maxAgeSeconds);
    }

    public Cookie buildRefreshCookie(String token, int maxAgeSeconds) {
        return build(REFRESH_COOKIE, token, "/api/auth", maxAgeSeconds);
    }

    public List<Cookie> buildClearCookies() {
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

    private Cookie build(String name, String value, String path, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath(path);
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Strict");
        return cookie;
    }
}