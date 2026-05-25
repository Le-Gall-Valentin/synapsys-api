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
    private final int accessMaxAgeSeconds;
    private final int refreshMaxAgeSeconds;

    public CookieService(SynapsysProperties properties) {
        this.secure = properties.cookie().secure();
        this.accessMaxAgeSeconds = (int) (properties.jwt().expiryMinutes() * 60L);
        this.refreshMaxAgeSeconds = (int) ((long) properties.refreshToken().expiryDays() * 86_400L);
    }

    public Cookie buildAccessCookie(String token) {
        return build(ACCESS_COOKIE, token, "/api", accessMaxAgeSeconds);
    }

    public Cookie buildRefreshCookie(String token) {
        return build(REFRESH_COOKIE, token, "/api/auth", refreshMaxAgeSeconds);
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