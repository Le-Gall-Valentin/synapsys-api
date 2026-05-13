package com.synapsys.api.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "synapsys")
public record SynapsysProperties(
    JwtProperties jwt,
    RefreshTokenProperties refreshToken,
    CookieProperties cookie,
    SeedProperties seed
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
        @DefaultValue("false") boolean enabled,
        @DefaultValue("admin") String username,
        @DefaultValue("admin@synapsys.dev") String email,
        @DefaultValue("changeme") String password
    ) {}
}