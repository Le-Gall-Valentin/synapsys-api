package com.synapsys.api.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@ConfigurationProperties(prefix = "synapsys")
public record SynapsysProperties(
    JwtProperties jwt,
    RefreshTokenProperties refreshToken,
    CookieProperties cookie,
    SeedProperties seed,
    CorsProperties cors
) {
    public record JwtProperties(
        String secret,
        @DefaultValue("15") int expiryMinutes
    ) {}

    public record RefreshTokenProperties(
        @DefaultValue("30") int expiryDays
    ) {}

    public record CookieProperties(
        @DefaultValue("true") boolean secure
    ) {}

    public record SeedProperties(
        @DefaultValue("admin") String username,
        @DefaultValue("admin@synapsys.dev") String email,
        String password
    ) {}

    public record CorsProperties(
        @DefaultValue("") List<String> allowedOrigins
    ) {}
}