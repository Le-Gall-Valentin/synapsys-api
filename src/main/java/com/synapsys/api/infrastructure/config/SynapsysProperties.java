package com.synapsys.api.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "synapsys")
public record SynapsysProperties(
    @Valid JwtProperties jwt,
    RefreshTokenProperties refreshToken,
    CookieProperties cookie,
    @Valid SeedProperties seed,
    CorsProperties cors,
    RateLimitProperties rateLimit
) {

    public record JwtProperties(
        @NotBlank String secret,
        @DefaultValue("15") int expiryMinutes,
        @DefaultValue("synapsys-api") String issuer,
        @DefaultValue("synapsys-api") String audience
    ) {}

    public record RefreshTokenProperties(
        @DefaultValue("30") int expiryDays,
        @DefaultValue("0 0 3 * * *") String purgeCron
    ) {}

    public record CookieProperties(
        @DefaultValue("true") boolean secure
    ) {}

    public record SeedProperties(
        @NotBlank String username,
        @NotBlank String email,
        @NotBlank String password
    ) {}

    public record CorsProperties(
        @DefaultValue("") List<String> allowedOrigins
    ) {}

    public record RateLimitProperties(
        @DefaultValue("") List<String> trustedProxies
    ) {}
}